package com.example.agents.hitl;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import com.example.agent.core.session.SessionId;
import io.github.markpollack.workflow.patterns.advisor.AgentLoopAdvisor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.AskUserQuestionTool.Question;
import org.springaicommunity.agent.tools.AskUserQuestionTool.QuestionHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.stereotype.Component;

/**
 * Step 8: Human-in-the-loop agent.
 *
 * <p>Same tools and prompt as Steps 04-07, but now the agent can pause mid-loop to ask the user for
 * clarification via {@link AskUserQuestionTool}. This bridges the blocking tool interface with the
 * Inspector's web UI using a {@link CompletableFuture} handoff.
 *
 * <p><b>What's new vs 04-07:</b>
 *
 * <ul>
 *   <li>{@link AskUserQuestionTool} — the LLM calls this tool when it needs user input
 *   <li>{@link InspectorQuestionHandler} — posts the question as an ASSISTANT message and blocks
 *       until the user responds via the web UI
 *   <li>Re-entrant {@code onMessage()} — detects pending questions and completes them instead of
 *       starting a new agent loop
 * </ul>
 *
 * <p><b>Why this matters:</b> Agents shouldn't guess when they're uncertain. Asking for
 * clarification leads to better outcomes and builds user trust.
 */
@Component
public class HumanInTheLoopHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(HumanInTheLoopHandler.class);

  private static final String SYSTEM_PROMPT =
      """
      You are Jarvis, a business dinner planning agent for Barcelona.

      Your job: find a restaurant, verify it meets ALL constraints, and book it.

      ## Constraints (check ALL of these)
      - Must be within company expense policy (use checkExpensePolicy tool)
      - Must accommodate dietary requirements (use checkDietaryOptions tool)
      - Must be reachable by the given time

      ## Process
      1. If the user's request is missing key details (neighborhood, budget, dietary needs,
         party size, date/time), use AskUserQuestionTool to ask for clarification
      2. Search for restaurants matching the criteria
      3. For each candidate, verify expense policy AND dietary options
      4. Present only restaurants that pass ALL checks
      5. Book when the user confirms

      ## Tool Selection Rules
      - Use AskUserQuestionTool when requirements are incomplete or ambiguous
      - Use searchRestaurants to find candidates
      - Use checkExpensePolicy BEFORE recommending any restaurant
      - Use checkDietaryOptions if dietary requirements are mentioned
      - Use bookTable only after user confirms the choice

      Never recommend a restaurant without checking expense policy first.
      Never guess dietary requirements — ask the user.
      """;

  private final ChatClient.Builder chatClientBuilder;
  private final RestaurantTools restaurantTools;
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  /** Pending questions keyed by session ID. Completed when the user responds. */
  private final ConcurrentHashMap<SessionId, CompletableFuture<String>> pendingAnswers =
      new ConcurrentHashMap<>();

  public HumanInTheLoopHandler(
      ChatClient.Builder chatClientBuilder, RestaurantTools restaurantTools) {
    this.chatClientBuilder = chatClientBuilder;
    this.restaurantTools = restaurantTools;
  }

  @Override
  public String getName() {
    return "08 — Human-in-the-Loop";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    // Check if this message is an answer to a pending question
    var pending = pendingAnswers.remove(session.id());
    if (pending != null) {
      log.info("Completing pending question for session {} with answer", session.id());
      pending.complete(message.text());
      return;
    }

    int turn = turnCounter.incrementAndGet();
    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Planning dinner (may ask questions)..."));

    // Create the AskUserQuestionTool with a handler bound to this session
    var questionHandler = new InspectorQuestionHandler(session, pendingAnswers);
    var askTool = AskUserQuestionTool.builder().questionHandler(questionHandler).build();

    var advisor =
        AgentLoopAdvisor.builder()
            .toolCallingManager(ToolCallingManager.builder().build())
            .maxTurns(15)
            .build();

    ChatClient chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(restaurantTools, askTool)
            .defaultAdvisors(advisor)
            .build();

    List<Message> history =
        session.getMessages().stream()
            .map(
                m ->
                    (Message)
                        (m.role() == Role.USER
                            ? new UserMessage(m.text())
                            : new AssistantMessage(m.text())))
            .toList();

    String reply = chatClient.prompt().messages(history).call().content();

    session.logEvent("assistant-reply-sent", Map.of("turn", turn, "reply", reply));
    session.appendMessage(Role.ASSISTANT, reply);
    session.updateState(buildState(turn, "Idle"));
  }

  private String buildState(int turn, String status) {
    return "## Jarvis — Human-in-the-Loop Agent\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | "
        + status
        + " |\n"
        + "| Tools | search, expensePolicy, dietary, book, **askUser** |\n"
        + "| Loop | AgentLoopAdvisor (max 15 turns) |\n";
  }

  /**
   * Bridges the blocking {@link QuestionHandler} to the Inspector web UI.
   *
   * <p>When the LLM calls {@link AskUserQuestionTool}, this handler:
   *
   * <ol>
   *   <li>Formats the question(s) as an ASSISTANT message visible in the chat
   *   <li>Registers a {@link CompletableFuture} for this session
   *   <li>Blocks until the user sends a response via the web UI
   * </ol>
   */
  static class InspectorQuestionHandler implements QuestionHandler {

    private final Session session;
    private final ConcurrentHashMap<SessionId, CompletableFuture<String>> pendingAnswers;

    InspectorQuestionHandler(
        Session session, ConcurrentHashMap<SessionId, CompletableFuture<String>> pendingAnswers) {
      this.session = session;
      this.pendingAnswers = pendingAnswers;
    }

    @Override
    public Map<String, String> handle(List<Question> questions) {
      // Format questions as a readable chat message
      var sb = new StringBuilder();
      for (var q : questions) {
        sb.append(q.question()).append("\n\n");
        if (q.options() != null) {
          for (int i = 0; i < q.options().size(); i++) {
            var opt = q.options().get(i);
            sb.append(String.format("%d. **%s** — %s%n", i + 1, opt.label(), opt.description()));
          }
        }
      }

      // Post the question as an assistant message
      session.appendMessage(Role.ASSISTANT, sb.toString().trim());
      session.updateState(
          "## Jarvis — Human-in-the-Loop Agent\n\n"
              + "| Field | Value |\n|-------|-------|\n"
              + "| Status | **Waiting for your answer** |\n");

      // Block until the user responds
      var future = new CompletableFuture<String>();
      pendingAnswers.put(session.id(), future);

      try {
        String answer = future.get();
        Map<String, String> answers = new HashMap<>();
        for (var q : questions) {
          answers.put(q.question(), answer);
        }
        return answers;
      } catch (Exception e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted waiting for user answer", e);
      }
    }
  }
}
