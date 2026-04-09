package com.example.agents.hooks;

import io.github.markpollack.hooks.decision.HookDecision;
import io.github.markpollack.hooks.event.BeforeToolCall;
import io.github.markpollack.hooks.registry.AgentHookRegistry;
import io.github.markpollack.hooks.spi.AgentHookProvider;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hook 2: Steering — blocks bookTable if checkExpensePolicy wasn't called first.
 *
 * <p>In Step 03 (guardrails), we asked the agent via the system prompt: "Never recommend a
 * restaurant without checking expense policy first." But the agent can ignore prompt rules.
 *
 * <p>This hook enforces the rule programmatically. If the agent tries to call {@code bookTable}
 * before calling {@code checkExpensePolicy}, the hook returns {@link HookDecision#block} — the tool
 * is never invoked, and the agent receives the block reason as the tool result.
 *
 * <p><b>What this teaches:</b> Hooks can enforce business rules that prompt engineering alone
 * cannot guarantee. Block is absolute — no amount of prompt jailbreaking can bypass it.
 */
@Component
public class ExpensePolicyProvider implements AgentHookProvider {

  private static final Logger log = LoggerFactory.getLogger(ExpensePolicyProvider.class);

  private final AtomicInteger blockCount = new AtomicInteger(0);

  @Override
  public void registerHooks(AgentHookRegistry registry) {
    registry.onTool(
        "bookTable",
        BeforeToolCall.class,
        20,
        event -> {
          boolean policyChecked =
              event.context().history().stream()
                  .anyMatch(r -> r.toolName().equals("checkExpensePolicy"));

          if (!policyChecked) {
            blockCount.incrementAndGet();
            log.warn("[Hook] BLOCKED bookTable — checkExpensePolicy has not been called yet");
            return HookDecision.block(
                "Booking blocked: expense policy must be checked before booking a table. "
                    + "Please call checkExpensePolicy first.");
          }

          log.info("[Hook] bookTable allowed — expense policy was checked");
          return HookDecision.proceed();
        });
  }

  public int getBlockCount() {
    return blockCount.get();
  }
}
