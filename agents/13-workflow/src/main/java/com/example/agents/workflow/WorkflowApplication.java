package com.example.agents.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Step 13: Workflow DSL — the "Stripe pattern."
 *
 * <p>Sandwiches the LLM between deterministic steps: gather context (zero tokens) → AI
 * recommendation (LLM reasoning) → validate (zero tokens, guaranteed correct).
 */
@SpringBootApplication
public class WorkflowApplication {

  private static final Logger log = LoggerFactory.getLogger(WorkflowApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(WorkflowApplication.class, args);
  }

  @Bean
  public CommandLineRunner runWorkflow(ChatClient.Builder chatClientBuilder) {
    return args -> {
      ChatClient chatClient = chatClientBuilder.build();
      DinnerPlanningWorkflow workflow = new DinnerPlanningWorkflow(chatClient);

      DinnerRequest request = new DinnerRequest("Eixample", null, 4, 30.0, "vegetarian");

      log.info("=== Step 13: Workflow DSL (Stripe Pattern) ===");
      log.info(
          "Request: {} in {}, {} guests, budget {}{}/person, dietary: {}",
          request.cuisine() != null ? request.cuisine() : "any cuisine",
          request.neighborhood(),
          request.partySize(),
          "\u20ac",
          request.budgetPerPerson(),
          request.dietary());

      long start = System.currentTimeMillis();
      DinnerResult result = workflow.run(request);
      long elapsed = System.currentTimeMillis() - start;

      log.info("--- Candidates ({}) ---", result.candidates().size());
      result
          .candidates()
          .forEach(
              r ->
                  log.info(
                      "  {} ({}, {}{}/person)",
                      r.get("name"),
                      r.get("neighborhood"),
                      "\u20ac",
                      r.get("pricePerPerson")));

      log.info("--- Recommendation ---");
      log.info("{}", result.recommendation());

      log.info("--- Validation ---");
      log.info("{}: {}", result.valid() ? "PASS" : "FAIL", result.validationMessage());
      log.info("Total time: {}ms", elapsed);
    };
  }
}
