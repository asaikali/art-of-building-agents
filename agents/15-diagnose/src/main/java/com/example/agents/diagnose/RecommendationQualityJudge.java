package com.example.agents.diagnose;

import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.llm.LLMJudge;
import org.springaicommunity.judge.result.Judgment;
import org.springaicommunity.judge.result.JudgmentStatus;
import org.springframework.ai.chat.client.ChatClient;

/** T2 LLM judge: evaluates recommendation quality. Only fires if T0 and T1 pass. */
public class RecommendationQualityJudge extends LLMJudge {

  public RecommendationQualityJudge(ChatClient.Builder chatClientBuilder) {
    super("RecommendationQuality", "Evaluates recommendation quality with LLM", chatClientBuilder);
  }

  @Override
  protected String buildPrompt(JudgmentContext context) {
    return String.format(
        """
        You are evaluating a restaurant recommendation agent.

        User goal: %s

        Agent output:
        %s

        Rate this recommendation:
        1. Does it clearly name a specific restaurant?
        2. Does it explain WHY this restaurant fits?
        3. Is the response concise and actionable?

        Respond with exactly one line:
        GOOD: <one-sentence reasoning>
        or
        POOR: <one-sentence reasoning>
        """,
        context.goal(), context.agentOutput().orElse("(no output)"));
  }

  @Override
  protected Judgment parseResponse(String response, JudgmentContext context) {
    String trimmed = response.trim();
    if (trimmed.toUpperCase().startsWith("GOOD")) {
      String reasoning = trimmed.length() > 5 ? trimmed.substring(5).trim() : "Good recommendation";
      if (reasoning.startsWith(":")) {
        reasoning = reasoning.substring(1).trim();
      }
      return Judgment.pass(reasoning);
    } else {
      String reasoning = trimmed.length() > 5 ? trimmed.substring(5).trim() : "Poor recommendation";
      if (reasoning.startsWith(":")) {
        reasoning = reasoning.substring(1).trim();
      }
      return Judgment.builder().status(JudgmentStatus.FAIL).reasoning(reasoning).build();
    }
  }
}
