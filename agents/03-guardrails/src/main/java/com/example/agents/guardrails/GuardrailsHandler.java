package com.example.agents.guardrails;

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
 * Step 3: Guardrails agent.
 *
 * <p>Same Jarvis agent, but now with:
 *
 * <ul>
 *   <li>More tools: search, expense policy, dietary check, booking
 *   <li>A richer system prompt with explicit constraints and tool selection rules
 *   <li>TODO: Replace ToolCallAdvisor with AgentLoopAdvisor for turn limits, stuck detection
 * </ul>
 *
 * <p><b>What's new vs 02:</b> More tools, constraint-aware system prompt.
 *
 * <p><b>What's still missing:</b> Turn limits (AgentLoopAdvisor), cost tracking.
 *
 * <p><b>Workshop note:</b> The system prompt is the single biggest quality driver. In the
 * code-coverage experiment, adding structure to the prompt (+0.07 quality) mattered more than any
 * other single change.
 */
@Component
public class GuardrailsHandler implements AgentHandler {

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

  public GuardrailsHandler(ChatClient.Builder chatClientBuilder, RestaurantTools restaurantTools) {
    // TODO: In the next step, replace ToolCallAdvisor with AgentLoopAdvisor
    // to add: maxTurns(15), stuck detection, grace turn recovery
    this.chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(restaurantTools)
            .defaultAdvisors(ToolCallAdvisor.builder().build())
            .build();
  }

  @Override
  public String getName() {
    return "03 — Guardrails";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Planning dinner — checking constraints..."));

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
    return "## Jarvis — Guardrails Agent\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | "
        + status
        + " |\n"
        + "| Tools | search, expensePolicy, dietary, book |\n"
        + "| Loop | ToolCallAdvisor (no limits yet) |\n"
        + "| Expense limit | €50/person |\n";
  }
}
