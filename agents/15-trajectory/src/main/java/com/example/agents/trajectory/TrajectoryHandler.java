package com.example.agents.trajectory;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import io.github.markpollack.workflow.patterns.advisor.AgentLoopAdvisor;
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
 * Runs Jarvis with tool call recording, then shows combined trajectory analysis + verdict in the
 * Inspector state panel.
 */
@Component
public class TrajectoryHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(TrajectoryHandler.class);

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
  private final CascadedJury jury;
  private final TrajectoryClassifier classifier = new TrajectoryClassifier();
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public TrajectoryHandler(ChatClient.Builder chatClientBuilder, RestaurantTools restaurantTools) {

    var advisor =
        AgentLoopAdvisor.builder()
            .toolCallingManager(ToolCallingManager.builder().build())
            .maxTurns(15)
            .build();

    this.chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(restaurantTools)
            .defaultAdvisors(advisor)
            .build();

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

  @Override
  public String getName() {
    return "15 \u2014 Trajectory";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();
    String goal = message.text();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", goal));
    session.updateState(buildRunningState(turn));

    // Clear any previous tool call records
    ToolCallTracker.getAndClear();

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

    // Capture tool calls
    List<String> toolCalls = ToolCallTracker.getAndClear();
    log.info("Tool calls recorded: {}", toolCalls);

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
  }

  private String buildRunningState(int turn) {
    return "## Jarvis \u2014 Trajectory-Analyzed Agent\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | Running agent + recording tool calls... |\n"
        + "| Tools | search, expensePolicy, dietary, book |\n"
        + "| Loop | AgentLoopAdvisor (max 15 turns) |\n";
  }

  private String buildAnalysisState(
      int turn, TrajectoryClassifier.TrajectoryAnalysis trajectory, Verdict verdict) {
    StringBuilder sb = new StringBuilder();
    sb.append("## Jarvis \u2014 Trajectory-Analyzed Agent\n\n");
    sb.append("| Field | Value |\n|-------|-------|\n");
    sb.append("| Turn | ").append(turn).append(" |\n");
    sb.append("| Status | Idle |\n\n");

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
