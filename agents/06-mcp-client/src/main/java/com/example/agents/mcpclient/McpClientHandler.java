package com.example.agents.mcpclient;

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
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * Step 6: MCP client agent.
 *
 * <p>Same Jarvis agent, but now the restaurant tools come from a remote MCP server instead of being
 * compiled into this app. The tools are discovered dynamically at startup via the MCP protocol.
 *
 * <p><b>What's new vs 05:</b> No {@code RestaurantTools.java} in this module. Tools are provided by
 * {@link ToolCallbackProvider} which the MCP client auto-configuration populates from connected MCP
 * servers.
 *
 * <p><b>What's the same:</b> System prompt, AgentLoopAdvisor, turn limits — all identical.
 *
 * <p><b>Why MCP matters:</b> Tool definitions live outside the agent. The MCP server can serve
 * multiple agents. Add new tools without redeploying the agent.
 */
@Component
public class McpClientHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(McpClientHandler.class);

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
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public McpClientHandler(
      ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider) {
    log.info("MCP tools discovered: {}", toolCallbackProvider);

    var advisor =
        AgentLoopAdvisor.builder()
            .toolCallingManager(ToolCallingManager.builder().build())
            .maxTurns(15)
            .build();

    this.chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultToolCallbacks(toolCallbackProvider)
            .defaultAdvisors(advisor)
            .build();
  }

  @Override
  public String getName() {
    return "06 — MCP Client";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Planning dinner (tools from MCP server)..."));

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
    return "## Jarvis — MCP Client Agent\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | "
        + status
        + " |\n"
        + "| Tools | from MCP server (localhost:8081) |\n"
        + "| Loop | AgentLoopAdvisor (max 15 turns) |\n";
  }
}
