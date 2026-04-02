package com.example.agents.qualitygate;

import io.github.markpollack.workflow.flows.AgentContext;
import io.github.markpollack.workflow.flows.Step;
import io.github.markpollack.workflow.flows.workflow.JudgeGate;
import io.github.markpollack.workflow.flows.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.judge.jury.CascadedJury;
import org.springaicommunity.judge.jury.MajorityVotingStrategy;
import org.springaicommunity.judge.jury.SimpleJury;
import org.springaicommunity.judge.jury.TierPolicy;
import org.springaicommunity.judge.jury.Verdict;
import org.springaicommunity.judge.result.Judgment;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Self-correction workflow: recommend → judge gate → on failure, reflect + revise → retry.
 *
 * <p>The gate pattern automates the eval → improve loop. Generate, judge, reflect, revise.
 */
public class QualityGateWorkflow {

  private static final Logger log = LoggerFactory.getLogger(QualityGateWorkflow.class);

  private final ChatClient chatClient;

  public QualityGateWorkflow(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  public String run(String userRequest) {
    // Step 1: AI recommendation
    Step<String, String> recommend =
        Step.named(
            "recommend",
            (ctx, input) -> {
              String prompt =
                  String.format(
                      """
                      You are Jarvis, a business dinner planning agent for Barcelona.

                      Available restaurants:
                      %s

                      Corporate expense limit: \u20ac%.0f per person.

                      User request: %s

                      Recommend ONE restaurant. Name it explicitly and explain why it fits.
                      Be concise (2-3 sentences). Start with the restaurant name.""",
                      RestaurantData.formatForPrompt(), RestaurantData.EXPENSE_LIMIT, input);

              // Check if there's feedback from a previous failed attempt
              String feedback = ctx.get(AgentContext.JUDGE_REFLECTION).orElse(null);
              if (feedback != null) {
                prompt += "\n\nIMPORTANT FEEDBACK FROM PREVIOUS ATTEMPT:\n" + feedback;
              }

              return chatClient.prompt().user(prompt).call().content();
            });

    // Build the judge gate
    CascadedJury jury = buildJury();
    JudgeGate<String> judgeGate = new JudgeGate<>(jury, 1.0);

    // Reflector: transform verdict into constructive feedback
    Step<String, String> reflector =
        Step.named(
            "reflect",
            (ctx, lastOutput) -> {
              Verdict verdict = judgeGate.lastVerdict();
              if (verdict == null) {
                return "The previous recommendation failed validation. Try a different restaurant.";
              }

              StringBuilder feedback = new StringBuilder();
              feedback.append("Your previous recommendation was rejected:\n");

              // Extract failure reasons from all sub-verdicts
              if (verdict.subVerdicts() != null) {
                for (Verdict sub : verdict.subVerdicts()) {
                  for (var entry : sub.individualByName().entrySet()) {
                    Judgment j = entry.getValue();
                    if (!j.pass()) {
                      feedback
                          .append("- ")
                          .append(entry.getKey())
                          .append(": ")
                          .append(j.reasoning())
                          .append("\n");
                    }
                  }
                }
              }
              for (var entry : verdict.individualByName().entrySet()) {
                Judgment j = entry.getValue();
                if (!j.pass()) {
                  feedback
                      .append("- ")
                      .append(entry.getKey())
                      .append(": ")
                      .append(j.reasoning())
                      .append("\n");
                }
              }

              feedback.append("\nPlease recommend a DIFFERENT restaurant that passes all checks.");
              return feedback.toString();
            });

    // Format result
    Step<String, String> formatResult =
        Step.named("format", (ctx, recommendation) -> recommendation);

    // Wire the workflow with gate
    return Workflow.<String, String>define("jarvis-with-quality-gate")
        .step(recommend)
        .gate(judgeGate)
        .onPass(formatResult)
        .onFail(recommend)
        .withReflector(reflector)
        .maxRetries(2)
        .end()
        .run(userRequest);
  }

  private CascadedJury buildJury() {
    SimpleJury tier0 =
        SimpleJury.builder()
            .judge(new ExpensePolicyJudge())
            .votingStrategy(new MajorityVotingStrategy())
            .build();

    return CascadedJury.builder()
        .tier("T0-ExpensePolicy", tier0, TierPolicy.REJECT_ON_ANY_FAIL)
        .build();
  }
}
