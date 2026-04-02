package com.example.agents.turnlimits;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import io.github.markpollack.workflow.patterns.advisor.AgentLoopAdvisor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/**
 * Step 4: Turn-limited agent.
 *
 * <p>This is the first step that uses an AgentWorks library. We replace Spring AI's bare {@code
 * ToolCallAdvisor} with {@code AgentLoopAdvisor} from agent-workflow.
 *
 * <p>{@code AgentLoopAdvisor} extends {@code ToolCallAdvisor} — same loop underneath — but adds:
 *
 * <ul>
 *   <li><b>maxTurns</b>: hard stop after N iterations, with a grace turn for a final summary
 *   <li><b>stuck detection</b>: SHA-256 hash tracking of tool calls, detects 5x consecutive
 *       repetition or A-B-A-B alternation patterns
 *   <li><b>cost limit</b>: track token usage, stop when estimated cost exceeds threshold
 *   <li><b>grace turn</b>: when max turns is reached, one additional tool-free LLM call allows the
 *       agent to summarize progress rather than cutting off mid-task
 * </ul>
 *
 * <p><b>What's new vs 03:</b> same tools, same prompt — but the loop is now governed.
 *
 * <p><b>What's new architecturally:</b> first dependency on an AgentWorks library (workflow-core).
 * Steps 01-03 were pure Spring AI.
 */
@Component
public class TurnLimitsHandler implements AgentHandler {

  private static final String SYSTEM_PROMPT =
      """
      You are Jarvis, a business dinner planning agent for Barcelona.

      Your job: find a restaurant, verify it meets ALL constraints, and book it.

      ## Constraints (check ALL of these)
      - Must be within company expense policy (use checkExpensePolicy tool)
      - Must accommodate dietary requirements (use checkDietaryOptions tool)
      - Must be reachable by the given time

      ## Process
      1. Ask clarifying questions if requirements are unclear
      2. Search for restaurants matching the criteria
      3. For each candidate, verify expense policy AND dietary options
      4. Present only restaurants that pass ALL checks
      5. Book when the user confirms

      ## Tool Selection Rules
      - Use searchRestaurants to find candidates
      - Use checkExpensePolicy BEFORE recommending any restaurant
      - Use checkDietaryOptions if dietary requirements are mentioned
      - Use bookTable only after user confirms the choice

      Never recommend a restaurant without checking expense policy first.
      """;

  private final ChatClient chatClient;
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public TurnLimitsHandler(ChatClient.Builder chatClientBuilder, RestaurantTools restaurantTools) {
    // AgentLoopAdvisor replaces ToolCallAdvisor — same loop, now governed
    var advisor = AgentLoopAdvisor.builder().maxTurns(15).build();

    this.chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(restaurantTools)
            .defaultAdvisors(advisor)
            .build();
  }

  @Override
  public String getName() {
    return "04 — Turn Limits";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Planning dinner (max 15 turns)..."));

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
    return "## Jarvis — Turn-Limited Agent\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | "
        + status
        + " |\n"
        + "| Tools | search, expensePolicy, dietary, book |\n"
        + "| Loop | AgentLoopAdvisor (max 15 turns) |\n"
        + "| Stuck detection | 5x repeat or A-B-A-B |\n"
        + "| Expense limit | €50/person |\n";
  }
}
