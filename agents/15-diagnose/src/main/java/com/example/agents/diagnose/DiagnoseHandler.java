package com.example.agents.diagnose;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import io.github.markpollack.journal.Journal;
import io.github.markpollack.journal.Run;
import io.github.markpollack.journal.event.CustomEvent;
import io.github.markpollack.journal.storage.JsonFileStorage;
import io.github.markpollack.workflow.patterns.advisor.AgentLoopAdvisor;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.jury.CascadedJury;
import org.springaicommunity.judge.jury.MajorityVotingStrategy;
import org.springaicommunity.judge.jury.SimpleJury;
import org.springaicommunity.judge.jury.TierPolicy;
import org.springaicommunity.judge.jury.Verdict;
import org.springaicommunity.judge.result.Judgment;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.stereotype.Component;

/**
 * Step 15: Diagnose — record, evaluate, and analyze agent behavior.
 *
 * <p>Combines three feedback signals in one handler:
 *
 * <ol>
 *   <li><b>Journal</b> — records every loop event (turns, tokens, cost) to JSONL via {@link
 *       JournalLoopListener}
 *   <li><b>Judge</b> — evaluates output correctness with a 3-tier {@link CascadedJury}
 *   <li><b>Trajectory</b> — classifies tool calls into semantic states, detects loops and hotspots
 * </ol>
 *
 * <p>The agent is free-form (ChatClient + AgentLoopAdvisor). After it runs, the three signals show
 * WHAT happened (journal), WHETHER it's correct (judge), and WHERE it wasted time (trajectory).
 */
@Component
public class DiagnoseHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(DiagnoseHandler.class);

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
  private final CascadedJury jury;
  private final TrajectoryClassifier classifier = new TrajectoryClassifier();
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public DiagnoseHandler(ChatClient.Builder chatClientBuilder, RestaurantTools restaurantTools) {
    this.chatClientBuilder = chatClientBuilder;
    this.restaurantTools = restaurantTools;

    // Same 3-tier jury as Step 14
    SimpleJury tier0 =
        SimpleJury.builder()
            .judge(new ExpensePolicyJudge())
            .votingStrategy(new MajorityVotingStrategy())
            .build();

    SimpleJury tier1 =
        SimpleJury.builder()
            .judge(new DietaryComplianceJudge())
            .votingStrategy(new MajorityVotingStrategy())
            .build();

    SimpleJury tier2 =
        SimpleJury.builder()
            .judge(new RecommendationQualityJudge(chatClientBuilder))
            .votingStrategy(new MajorityVotingStrategy())
            .build();

    this.jury =
        CascadedJury.builder()
            .tier("T0-ExpensePolicy", tier0, TierPolicy.REJECT_ON_ANY_FAIL)
            .tier("T1-DietaryCompliance", tier1, TierPolicy.ACCEPT_ON_ALL_PASS)
            .tier("T2-RecommendationQuality", tier2, TierPolicy.FINAL_TIER)
            .build();
  }

  @PostConstruct
  void configureJournal() {
    Journal.configure(new JsonFileStorage(Path.of(".agent-journal")));
    log.info("Journal configured — output in .agent-journal/");
  }

  @Override
  public String getName() {
    return "15 \u2014 Diagnose";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();
    String goal = message.text();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", goal));
    session.updateState(buildRunningState(turn));

    // Clear any previous tool call records
    ToolCallTracker.getAndClear();

    // Create a journal run for this interaction
    try (Run run =
        Journal.run("jarvis-restaurant-agent")
            .name("turn-" + turn)
            .task("dinner-recommendation")
            .agent("jarvis")
            .start()) {

      // Wire journal listener into AgentLoopAdvisor
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

      Instant start = Instant.now();
      String reply = chatClient.prompt().messages(history).call().content();
      Duration elapsed = Duration.between(start, Instant.now());

      // Capture tool calls for trajectory analysis
      List<String> toolCalls = ToolCallTracker.getAndClear();
      log.info("Tool calls recorded: {}", toolCalls);

      run.logEvent(CustomEvent.of("assistant_reply", Map.of("turn", turn, "reply", reply)));

      session.appendMessage(Role.ASSISTANT, reply);

      // Trajectory analysis
      TrajectoryClassifier.TrajectoryAnalysis trajectory = classifier.classify(toolCalls);

      // Evaluate with CascadedJury
      JudgmentContext judgmentCtx =
          JudgmentContext.builder()
              .goal(goal)
              .agentOutput(reply)
              .executionTime(elapsed)
              .startedAt(start)
              .build();

      Verdict verdict = jury.vote(judgmentCtx);

      log.info(
          "Trajectory: {} states, {} loops, {:.0f}% efficiency | Verdict: {}",
          trajectory.sequence().size(),
          trajectory.loops().size(),
          trajectory.efficiency() * 100,
          verdict.aggregated().status());

      session.updateState(buildAnalysisState(turn, trajectory, verdict));

      log.info("Journal run {} completed for turn {}", run.id(), turn);
    }
  }

  private String buildRunningState(int turn) {
    return "## Jarvis \u2014 Diagnose Agent\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | Running agent + recording... |\n"
        + "| Tools | search, expensePolicy, dietary, book |\n"
        + "| Loop | AgentLoopAdvisor (max 15 turns) |\n"
        + "| Journal | .agent-journal/ (JSONL) |\n";
  }

  private String buildAnalysisState(
      int turn, TrajectoryClassifier.TrajectoryAnalysis trajectory, Verdict verdict) {
    StringBuilder sb = new StringBuilder();
    sb.append("## Jarvis \u2014 Diagnose Agent\n\n");
    sb.append("| Field | Value |\n|-------|-------|\n");
    sb.append("| Turn | ").append(turn).append(" |\n");
    sb.append("| Status | Idle |\n");
    sb.append("| Journal | .agent-journal/ (JSONL) |\n\n");

    // Trajectory section
    sb.append(classifier.formatMarkdown(trajectory));
    sb.append("\n");

    // Verdict section
    sb.append("## Verdict: ").append(verdict.aggregated().status()).append("\n\n");
    sb.append("| Judge | Status | Reasoning |\n");
    sb.append("|-------|--------|----------|\n");

    for (Map.Entry<String, Judgment> entry : verdict.individualByName().entrySet()) {
      Judgment j = entry.getValue();
      sb.append("| ")
          .append(entry.getKey())
          .append(" | ")
          .append(j.status())
          .append(" | ")
          .append(j.reasoning())
          .append(" |\n");
    }

    if (verdict.subVerdicts() != null) {
      for (Verdict sub : verdict.subVerdicts()) {
        for (Map.Entry<String, Judgment> entry : sub.individualByName().entrySet()) {
          Judgment j = entry.getValue();
          sb.append("| ")
              .append(entry.getKey())
              .append(" | ")
              .append(j.status())
              .append(" | ")
              .append(j.reasoning())
              .append(" |\n");
        }
      }
    }

    return sb.toString();
  }
}
