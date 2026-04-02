package com.example.agents.a2a.expense;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import java.util.List;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Expense policy checker exposed as an A2A (Agent-to-Agent) agent.
 *
 * <p>This is a standalone Spring Boot app that runs on port 8082 and exposes:
 *
 * <ul>
 *   <li>{@code /.well-known/agent-card.json} — agent discovery endpoint
 *   <li>{@code /} — A2A JSON-RPC message endpoint
 * </ul>
 *
 * <p>Other agents (like Jarvis) discover this agent via its {@link AgentCard} and send expense
 * policy check requests using the A2A protocol.
 */
@SpringBootApplication
public class ExpensePolicyApplication {

  private static final String SYSTEM_PROMPT =
      """
      You are a corporate expense policy assistant for Barcelona business dinners.

      You have a tool to check if a restaurant's price is within the corporate expense limit.
      When asked about expense policy, ALWAYS use the checkExpensePolicy tool to give an accurate answer.

      Be concise and factual. Return the policy check result clearly.
      """;

  public static void main(String[] args) {
    SpringApplication.run(ExpensePolicyApplication.class, args);
  }

  @Bean
  public AgentCard agentCard(
      @Value("${server.port:8082}") int port,
      @Value("${server.servlet.context-path:/}") String contextPath) {

    return new AgentCard.Builder()
        .name("Expense Policy Agent")
        .description(
            "Checks if restaurant prices are within corporate expense policy limits "
                + "for Barcelona business dinners")
        .url("http://localhost:" + port + contextPath)
        .version("1.0.0")
        .capabilities(new AgentCapabilities.Builder().streaming(false).build())
        .defaultInputModes(List.of("text"))
        .defaultOutputModes(List.of("text"))
        .skills(
            List.of(
                new AgentSkill.Builder()
                    .id("expense_check")
                    .name("Check expense policy")
                    .description(
                        "Verify if a restaurant price per person is within corporate limits")
                    .tags(List.of("expense", "policy", "budget"))
                    .examples(
                        List.of(
                            "Is 35 EUR per person within policy for 4 guests?",
                            "Check expense policy for 55 EUR per person, party of 6"))
                    .build()))
        .protocolVersion("0.3.0")
        .build();
  }

  @Bean
  public AgentExecutor agentExecutor(
      ChatClient.Builder chatClientBuilder, ExpensePolicyTools expensePolicyTools) {

    ChatClient chatClient =
        chatClientBuilder.defaultSystem(SYSTEM_PROMPT).defaultTools(expensePolicyTools).build();

    return new DefaultAgentExecutor(
        chatClient,
        (chat, requestContext) -> {
          String userMessage =
              DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
          return chat.prompt().user(userMessage).call().content();
        });
  }
}
