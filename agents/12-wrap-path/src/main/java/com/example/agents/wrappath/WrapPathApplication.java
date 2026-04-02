package com.example.agents.wrappath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Step 12: Wrap path — Claude Code as a Spring-managed agent.
 *
 * <p>Instead of building an agent loop with Spring AI's ChatClient + tools (Steps 01-11), this step
 * wraps an existing CLI agent (Claude Code) using {@code agent-client}. Same Jarvis domain, same
 * restaurant data — but the agent loop runs inside Claude Code, not in our Java process.
 *
 * <p><b>Why this matters:</b> You don't always have to build the agent from scratch. If you already
 * have Claude Code (or Gemini CLI), wrap it as a Spring component and get the same observability
 * (journal), evaluation (judges), and orchestration capabilities.
 *
 * <p>Requires the {@code claude} CLI to be installed and available on PATH.
 */
@SpringBootApplication
public class WrapPathApplication {

  private static final Logger log = LoggerFactory.getLogger(WrapPathApplication.class);

  private static final String DEFAULT_GOAL =
      "Find a restaurant in Eixample for 4 people with a team lunch budget "
          + "(30 EUR per person). One person is vegetarian. "
          + "Recommend the best option and explain why it fits the requirements.";

  public static void main(String[] args) {
    SpringApplication.run(WrapPathApplication.class, args);
  }

  @Bean
  public CommandLineRunner runAgent(AgentClient.Builder agentClientBuilder) {
    return args -> {
      String goal = args.length > 0 ? String.join(" ", args) : DEFAULT_GOAL;

      log.info("=== Step 12: Wrap Path ===");
      log.info("Goal: {}", goal);
      log.info("Sending to Claude Code via agent-client...");

      AgentClient agentClient = agentClientBuilder.build();
      AgentClientResponse response = agentClient.run(goal);

      if (response.isSuccessful()) {
        log.info("=== Agent Response ===");
        log.info("{}", response.getResult());
      } else {
        log.error("Agent execution failed: {}", response.getResult());
      }
    };
  }
}
