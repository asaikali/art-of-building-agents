package com.example.agents.a2a.client;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * A2A client tool — sends expense policy queries to the remote Expense Policy Agent.
 *
 * <p>At construction time, discovers the remote agent via its {@link AgentCard} at {@code
 * http://localhost:8082/.well-known/agent-card.json}. When Jarvis calls {@code
 * checkExpensePolicyRemote}, this tool sends the request via A2A protocol and waits for the
 * response.
 */
public class RemoteExpenseAgent {

  private static final Logger log = LoggerFactory.getLogger(RemoteExpenseAgent.class);

  private final AgentCard agentCard;

  public RemoteExpenseAgent(String agentUrl) {
    log.info("Resolving A2A agent card from: {}", agentUrl);
    try {
      String path = new URI(agentUrl).getPath();
      this.agentCard = A2A.getAgentCard(agentUrl, path + ".well-known/agent-card.json", null);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to resolve A2A agent card from: " + agentUrl, e);
    }
    log.info("Discovered agent: {} — {}", agentCard.name(), agentCard.description());
  }

  @Tool(
      description =
          "Check corporate expense policy by asking the remote Expense Policy Agent. "
              + "Provide the price per person and party size.")
  public String checkExpensePolicyRemote(
      @ToolParam(
              description =
                  "The expense policy question, e.g. 'Is 35 EUR per person within policy for 4 guests?'")
          String query) {

    log.info("Sending A2A message to Expense Policy Agent: {}", query);

    try {
      Message message =
          new Message.Builder()
              .role(Message.Role.USER)
              .parts(List.of(new TextPart(query, null)))
              .build();

      CompletableFuture<String> responseFuture = new CompletableFuture<>();

      BiConsumer<ClientEvent, AgentCard> consumer =
          (event, card) -> {
            if (event instanceof TaskEvent taskEvent) {
              var completedTask = taskEvent.getTask();
              log.info("Received task response: status={}", completedTask.getStatus().state());

              StringBuilder sb = new StringBuilder();
              if (completedTask.getArtifacts() != null) {
                for (Artifact artifact : completedTask.getArtifacts()) {
                  if (artifact.parts() != null) {
                    for (Part<?> part : artifact.parts()) {
                      if (part instanceof TextPart textPart) {
                        sb.append(textPart.getText());
                      }
                    }
                  }
                }
              }
              responseFuture.complete(sb.toString());
            }
          };

      ClientConfig clientConfig =
          new ClientConfig.Builder().setAcceptedOutputModes(List.of("text")).build();
      Client client =
          Client.builder(agentCard)
              .clientConfig(clientConfig)
              .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
              .addConsumers(List.of(consumer))
              .build();

      client.sendMessage(message);

      String result = responseFuture.get(60, TimeUnit.SECONDS);
      log.info("Expense Policy Agent response: {}", result);
      return result;
    } catch (Exception e) {
      log.error("Error communicating with Expense Policy Agent: {}", e.getMessage());
      return "Error communicating with Expense Policy Agent: " + e.getMessage();
    }
  }
}
