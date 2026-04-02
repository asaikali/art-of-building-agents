package com.example.agents.judge;

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
 * Runs Jarvis, then evaluates the response with a 3-tier CascadedJury. Verdict shown in the
 * Inspector state panel.
 */
@Component
public class JudgeHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(JudgeHandler.class);

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
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public JudgeHandler(ChatClient.Builder chatClientBuilder, RestaurantTools restaurantTools) {

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

    // Build 3-tier cascaded jury
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
    return "14 \u2014 Judge";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();
    String goal = message.text();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", goal));
    session.updateState(buildState(turn, "Running Jarvis agent...", null));

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

    session.appendMessage(Role.ASSISTANT, reply);

    // Evaluate with CascadedJury
    session.updateState(buildState(turn, "Evaluating with CascadedJury...", null));

    JudgmentContext judgmentCtx =
        JudgmentContext.builder()
            .goal(goal)
            .agentOutput(reply)
            .executionTime(elapsed)
            .startedAt(start)
            .build();

    Verdict verdict = jury.vote(judgmentCtx);

    log.info("Verdict: {}", verdict.aggregated().status());
    session.logEvent(
        "verdict",
        Map.of(
            "turn", turn,
            "status", verdict.aggregated().status().name(),
            "reasoning", verdict.aggregated().reasoning()));

    session.updateState(buildState(turn, "Idle", verdict));
  }

  private String buildState(int turn, String status, Verdict verdict) {
    StringBuilder sb = new StringBuilder();
    sb.append("## Jarvis \u2014 Judge-Evaluated Agent\n\n");
    sb.append("| Field | Value |\n|-------|-------|\n");
    sb.append("| Turn | ").append(turn).append(" |\n");
    sb.append("| Status | ").append(status).append(" |\n");
    sb.append("| Tools | search, expensePolicy, dietary, book |\n");
    sb.append("| Loop | AgentLoopAdvisor (max 15 turns) |\n");
    sb.append("| Jury | CascadedJury (3 tiers) |\n\n");

    if (verdict != null) {
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

      // Also show sub-verdict details if present
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
    }

    return sb.toString();
  }
}
