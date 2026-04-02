package com.example.agents.journal;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import io.github.markpollack.journal.Journal;
import io.github.markpollack.journal.Run;
import io.github.markpollack.journal.storage.JsonFileStorage;
import io.github.markpollack.workflow.patterns.advisor.AgentLoopAdvisor;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.stereotype.Component;

/**
 * Step 5: Journal-recording agent.
 *
 * <p>Same tools and prompt as Step 04, but now every agent interaction is recorded to an
 * append-only JSONL file via agent-journal.
 *
 * <p><b>What's new vs 04:</b> Each message creates a journal {@link Run} that records loop events
 * (turns started/completed, tokens used, estimated cost, termination reason). The {@link
 * JournalLoopListener} bridges {@link AgentLoopAdvisor} events into the journal.
 *
 * <p><b>Why this matters:</b> This is the seam between "build" and "measure." The JSONL data feeds
 * trajectory analysis, heatmaps, and judge scoring. Without recording, you can't improve.
 *
 * <p>Journal output is written to {@code .agent-journal/} in the working directory. After running
 * this step, look at the JSONL files to see exactly what the agent did.
 */
@Component
public class JournalHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(JournalHandler.class);

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

  private final ChatClient.Builder chatClientBuilder;
  private final RestaurantTools restaurantTools;
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public JournalHandler(ChatClient.Builder chatClientBuilder, RestaurantTools restaurantTools) {
    this.chatClientBuilder = chatClientBuilder;
    this.restaurantTools = restaurantTools;
  }

  @PostConstruct
  void configureJournal() {
    Journal.configure(new JsonFileStorage(Path.of(".agent-journal")));
    log.info("Journal configured — output in .agent-journal/");
  }

  @Override
  public String getName() {
    return "05 — Journal";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Planning dinner (recording to journal)..."));

    // Create a journal run for this interaction
    try (Run run =
        Journal.run("jarvis-dinner-planning")
            .config("model", "gpt-4o")
            .config("maxTurns", 15)
            .tag("step", "05-journal")
            .tag("turn", String.valueOf(turn))
            .start()) {

      // Wire the journal listener into AgentLoopAdvisor
      var advisor =
          AgentLoopAdvisor.builder()
              .toolCallingManager(ToolCallingManager.builder().build())
              .maxTurns(15)
              .listener(new JournalLoopListener(run))
              .build();

      ChatClient chatClient =
          chatClientBuilder
              .defaultSystem(SYSTEM_PROMPT)
              .defaultTools(restaurantTools)
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

      run.logEvent(
          io.github.markpollack.journal.event.CustomEvent.of(
              "assistant_reply", Map.of("turn", turn, "reply", reply)));

      session.logEvent("assistant-reply-sent", Map.of("turn", turn, "reply", reply));
      session.appendMessage(Role.ASSISTANT, reply);
      session.updateState(buildState(turn, "Idle — check .agent-journal/ for recording"));

      log.info("Journal run {} completed for turn {}", run.id(), turn);
    }
  }

  private String buildState(int turn, String status) {
    return "## Jarvis — Journal-Recording Agent\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | "
        + status
        + " |\n"
        + "| Tools | search, expensePolicy, dietary, book |\n"
        + "| Loop | AgentLoopAdvisor (max 15 turns) |\n"
        + "| Journal | .agent-journal/ (JSONL) |\n";
  }
}
