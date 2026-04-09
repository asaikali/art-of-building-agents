package com.example.agents.hooks;

import io.github.markpollack.hooks.decision.HookDecision;
import io.github.markpollack.hooks.event.AfterToolCall;
import io.github.markpollack.hooks.event.BeforeToolCall;
import io.github.markpollack.hooks.registry.AgentHookRegistry;
import io.github.markpollack.hooks.spi.AgentHookProvider;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hook 1: Observation — logs every tool call with timing.
 *
 * <p>This is the simplest hook: it observes tool execution without changing behavior. Every
 * BeforeToolCall logs the tool name and input; every AfterToolCall logs the duration.
 *
 * <p><b>What this teaches:</b> Hooks give you visibility into what the agent is actually doing,
 * without modifying the system prompt or the tools themselves.
 */
@Component
public class ToolCallLoggingProvider implements AgentHookProvider {

  private static final Logger log = LoggerFactory.getLogger(ToolCallLoggingProvider.class);

  private final AtomicInteger callCount = new AtomicInteger(0);

  @Override
  public void registerHooks(AgentHookRegistry registry) {
    registry.on(
        BeforeToolCall.class,
        10,
        event -> {
          int call = callCount.incrementAndGet();
          log.info(
              "[Hook] Tool call #{}: {} — input: {}", call, event.toolName(), event.toolInput());
          return HookDecision.proceed();
        });

    registry.on(
        AfterToolCall.class,
        10,
        event -> {
          log.info(
              "[Hook] Tool {} completed in {}ms", event.toolName(), event.duration().toMillis());
          return HookDecision.proceed();
        });
  }

  public int getCallCount() {
    return callCount.get();
  }
}
