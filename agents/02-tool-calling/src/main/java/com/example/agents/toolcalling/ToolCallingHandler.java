package com.example.agents.toolcalling;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
// DefaultToolCallingManager not needed — ToolCallAdvisor.builder() uses sensible defaults
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/**
 * Step 2: Tool-calling agent.
 *
 * <p>This is the first real agent — it has a system prompt (identity), a tool (searchRestaurants),
 * and the ToolCallAdvisor which provides the agent loop.
 *
 * <p>When the LLM decides it needs restaurant data, it generates a tool call. ToolCallAdvisor
 * intercepts it, executes the tool, feeds the result back, and loops until the LLM responds with
 * text.
 *
 * <p><b>What's new vs basic-chatbot:</b> system prompt + tool + ToolCallAdvisor loop.
 *
 * <p><b>What's missing:</b> no turn limit, no cost tracking — the loop runs until the LLM stops.
 */
@Component
public class ToolCallingHandler implements AgentHandler {

  private static final String SYSTEM_PROMPT =
      """
      You are Jarvis, a business dinner planning assistant for Barcelona.
      Help the user find restaurants that match their requirements.
      Use the searchRestaurants tool to find options.
      Be concise and helpful.
      """;

  private final ChatClient chatClient;
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public ToolCallingHandler(ChatClient.Builder chatClientBuilder, RestaurantTools restaurantTools) {
    this.chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(restaurantTools)
            .defaultAdvisors(ToolCallAdvisor.builder().build())
            .build();
  }

  @Override
  public String getName() {
    return "02 — Tool Calling";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Calling LLM with tools..."));

    // Build conversation history from session messages
    List<Message> history =
        session.getMessages().stream()
            .map(
                m ->
                    (Message)
                        (m.role() == Role.USER
                            ? new UserMessage(m.text())
                            : new AssistantMessage(m.text())))
            .toList();

    // Call the LLM — ToolCallAdvisor handles the tool execution loop automatically
    String reply = chatClient.prompt().messages(history).call().content();

    session.logEvent("assistant-reply-sent", Map.of("turn", turn, "reply", reply));
    session.appendMessage(Role.ASSISTANT, reply);
    session.updateState(buildState(turn, "Idle"));
  }

  private String buildState(int turn, String status) {
    return "## Jarvis — Tool Calling Agent\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | "
        + status
        + " |\n"
        + "| Tools | searchRestaurants |\n"
        + "| Loop | ToolCallAdvisor (no limits) |\n";
  }
}
