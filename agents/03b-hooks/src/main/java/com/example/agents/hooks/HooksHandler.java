package com.example.agents.hooks;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import io.github.markpollack.hooks.decision.HookContext;
import io.github.markpollack.hooks.registry.AgentHookRegistry;
import io.github.markpollack.hooks.spring.callback.HookedTools;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * Step 3b: Hooks agent.
 *
 * <p>Same Jarvis agent as Step 03 (guardrails), but now with programmatic hooks:
 *
 * <ul>
 *   <li>{@link ToolCallLoggingProvider} — observes every tool call (logging + counting)
 *   <li>{@link ExpensePolicyProvider} — blocks bookTable if expense policy wasn't checked
 *   <li>{@link CostGuardProvider} — tracks per-tool timing after each call
 * </ul>
 *
 * <p><b>What's new vs 03:</b> Tools are wrapped with {@link HookedTools#wrap}, which intercepts
 * every call. The system prompt is simpler — we don't need "never recommend without checking
 * expense policy" because the hook enforces it.
 *
 * <p><b>Key difference:</b> In Step 03, the expense policy rule was a prompt suggestion the agent
 * could ignore. Here, it's a programmatic gate the agent cannot bypass.
 */
@Component
public class HooksHandler implements AgentHandler {

  private static final String SYSTEM_PROMPT =
      """
      You are Jarvis, a business dinner planning agent for Barcelona.

      Your job: find a restaurant, verify it meets ALL constraints, and book it.

      ## Constraints
      - Must be within company expense policy (use checkExpensePolicy tool)
      - Must accommodate dietary requirements (use checkDietaryOptions tool)

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
      """;

  private final ChatClient chatClient;
  private final AgentHookRegistry registry;
  private final RestaurantTools restaurantTools;
  private final AtomicInteger turnCounter = new AtomicInteger(0);
  private final ToolCallLoggingProvider loggingProvider;
  private final ExpensePolicyProvider expensePolicyProvider;
  private final CostGuardProvider costGuardProvider;

  public HooksHandler(
      ChatClient.Builder chatClientBuilder,
      RestaurantTools restaurantTools,
      AgentHookRegistry registry,
      ToolCallLoggingProvider loggingProvider,
      ExpensePolicyProvider expensePolicyProvider,
      CostGuardProvider costGuardProvider) {
    this.registry = registry;
    this.restaurantTools = restaurantTools;
    this.loggingProvider = loggingProvider;
    this.expensePolicyProvider = expensePolicyProvider;
    this.costGuardProvider = costGuardProvider;

    // Tools are NOT set here — they're wrapped per-invocation with a fresh HookContext
    this.chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultAdvisors(ToolCallAdvisor.builder().build())
            .build();
  }

  @Override
  public String getName() {
    return "03b — Hooks";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Planning dinner — hooks active..."));

    // Fresh HookContext per invocation — isolates tool call history per turn
    HookContext hookContext = new HookContext();
    ToolCallbackProvider hookedTools = HookedTools.wrap(registry, hookContext, restaurantTools);

    List<Message> history =
        session.getMessages().stream()
            .map(
                m ->
                    (Message)
                        (m.role() == Role.USER
                            ? new UserMessage(m.text())
                            : new AssistantMessage(m.text())))
            .toList();

    String reply =
        chatClient.prompt().messages(history).toolCallbacks(hookedTools).call().content();

    session.logEvent("assistant-reply-sent", Map.of("turn", turn, "reply", reply));
    session.appendMessage(Role.ASSISTANT, reply);
    session.updateState(buildState(turn, "Idle"));
  }

  private String buildState(int turn, String status) {
    StringBuilder sb = new StringBuilder();
    sb.append("## Jarvis — Hooks Agent\n\n");
    sb.append("| Field | Value |\n|-------|-------|\n");
    sb.append("| Turn | ").append(turn).append(" |\n");
    sb.append("| Status | ").append(status).append(" |\n");
    sb.append("| Tools | search, expensePolicy, dietary, book |\n");
    sb.append("| Loop | ToolCallAdvisor (no limits yet) |\n");
    sb.append("| Expense limit | €50/person |\n\n");

    // Hook activity table
    sb.append("### Hook Activity\n\n");
    sb.append("| Metric | Value |\n|--------|-------|\n");
    sb.append("| Tool calls logged | ").append(loggingProvider.getCallCount()).append(" |\n");
    sb.append("| Bookings blocked | ").append(expensePolicyProvider.getBlockCount()).append(" |\n");

    Map<String, Long> counts = costGuardProvider.getCallCounts();
    Map<String, Duration> durations = costGuardProvider.getTotalDurations();
    if (!counts.isEmpty()) {
      sb.append("\n### Per-Tool Metrics\n\n");
      sb.append("| Tool | Calls | Total Time |\n|------|-------|------------|\n");
      counts.forEach(
          (tool, count) -> {
            Duration d = durations.getOrDefault(tool, Duration.ZERO);
            sb.append("| ").append(tool).append(" | ").append(count);
            sb.append(" | ").append(d.toMillis()).append("ms |\n");
          });
    }

    return sb.toString();
  }
}
