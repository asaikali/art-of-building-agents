package com.example.agents.qualitygate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Step 16: Quality Gate — self-correction loop.
 *
 * <p>The agent generates a recommendation, a judge gate evaluates it, and on failure, the agent
 * revises with feedback. The default goal deliberately triggers a failure: "Find a restaurant in
 * Paral-lel for a team lunch" — Tickets Bar at EUR 75 fails expense policy (EUR 50 limit). The
 * agent self-corrects on the second attempt.
 */
@SpringBootApplication
public class QualityGateApplication {

  private static final Logger log = LoggerFactory.getLogger(QualityGateApplication.class);

  private static final String DEFAULT_GOAL =
      "Find a restaurant in Paral\u00b7lel for a team lunch with 6 people. "
          + "We need a lively atmosphere.";

  public static void main(String[] args) {
    SpringApplication.run(QualityGateApplication.class, args);
  }

  @Bean
  public CommandLineRunner runQualityGate(ChatClient.Builder chatClientBuilder) {
    return args -> {
      String goal = args.length > 0 ? String.join(" ", args) : DEFAULT_GOAL;

      ChatClient chatClient = chatClientBuilder.build();
      QualityGateWorkflow workflow = new QualityGateWorkflow(chatClient);

      log.info("=== Step 16: Quality Gate (Self-Correction Loop) ===");
      log.info("Goal: {}", goal);
      log.info(
          "Note: This goal deliberately targets Paral\u00b7lel where Tickets Bar (\u20ac75) "
              + "exceeds the \u20ac50 expense limit. Watch the agent self-correct.\n");

      String result = workflow.run(goal);

      log.info("=== Final Result ===");
      log.info("{}", result);
    };
  }
}
