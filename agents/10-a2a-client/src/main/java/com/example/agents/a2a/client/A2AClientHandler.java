package com.example.agents.a2a.client;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import io.github.markpollack.workflow.patterns.advisor.AgentLoopAdvisor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Step 10: A2A protocol — Jarvis with remote expense policy agent.
 *
 * <p>Jarvis has local restaurant tools (search, dietary, book) but delegates expense policy checks
 * to a remote A2A agent running on port 8082. The remote agent is discovered at startup via its
 * {@code AgentCard}.
 *
 * <p><b>What's new vs Step 09:</b>
 *
 * <ul>
 *   <li>Expense policy checking is no longer a local tool — it's a remote A2A agent
 *   <li>{@link RemoteExpenseAgent} discovers the agent via {@code /.well-known/agent-card.json}
 *   <li>Communication uses Google's Agent-to-Agent (A2A) protocol over JSON-RPC
 * </ul>
 */
@Component
public class A2AClientHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(A2AClientHandler.class);

  private static final String SYSTEM_PROMPT =
      """
      You are Jarvis, a business dinner planning assistant for Barcelona.

      ## Available Tools
      - searchRestaurants — find restaurants by neighborhood or cuisine
      - checkDietaryOptions — verify dietary requirements at a restaurant
      - bookTable — book a reservation
      - checkExpensePolicyRemote — check if a price is within corporate expense limits
        (this calls a remote Expense Policy Agent via A2A protocol)

      ## Process
      1. Search for restaurants matching the user's criteria
      2. For candidates, check expense policy using checkExpensePolicyRemote
      3. Check dietary options if the user has dietary requirements
      4. Present qualifying restaurants to the user
      5. Book a table when the user decides

      ## Rules
      - ALWAYS check expense policy before recommending a restaurant
      - Include price per person AND party size when checking expense policy
      - Only recommend restaurants that pass ALL checks
      """;

  private final ChatClient chatClient;
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public A2AClientHandler(
      ChatClient.Builder chatClientBuilder,
      RestaurantTools restaurantTools,
      @Value("${remote.expense-agent.url:http://localhost:8082/}") String expenseAgentUrl) {

    var remoteExpenseAgent = new RemoteExpenseAgent(expenseAgentUrl);

    var agentLoop =
        AgentLoopAdvisor.builder()
            .toolCallingManager(ToolCallingManager.builder().build())
            .maxTurns(15)
            .build();

    this.chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(restaurantTools, remoteExpenseAgent)
            .defaultAdvisors(agentLoop)
            .build();
  }

  @Override
  public String getName() {
    return "10 — A2A Protocol";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Processing (may call remote Expense Policy Agent)..."));

    List<Message> history =
        session.getMessages().stream()
            .map(
                m ->
                    (Message)
                        (m.role() == Role.USER
                            ? new UserMessage(m.text())
                            : new AssistantMessage(m.text())))
            .toList();

    String reply = chatClient.prompt().messages(history).call().content();

    session.logEvent("assistant-reply-sent", Map.of("turn", turn, "reply", reply));
    session.appendMessage(Role.ASSISTANT, reply);
    session.updateState(buildState(turn, "Idle"));
  }

  private String buildState(int turn, String status) {
    return "## Jarvis — A2A Protocol\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | "
        + status
        + " |\n"
        + "| Local Tools | search, dietary, book |\n"
        + "| Remote Agent | Expense Policy (A2A, port 8082) |\n"
        + "| Loop | AgentLoopAdvisor (max 15 turns) |\n";
  }
}
