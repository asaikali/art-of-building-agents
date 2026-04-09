package com.example.agents.hooks;

import io.github.markpollack.hooks.decision.HookDecision;
import io.github.markpollack.hooks.event.AfterToolCall;
import io.github.markpollack.hooks.registry.AgentHookRegistry;
import io.github.markpollack.hooks.spi.AgentHookProvider;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hook 3: After-event tracking — accumulates per-tool timing and call counts.
 *
 * <p>Runs after every tool call completes, tracking how many times each tool was called and how
 * long each took. This data feeds the state panel in the Inspector UI.
 *
 * <p><b>What this teaches:</b> After-hooks run in reverse priority order (cleanup semantics). They
 * observe results without changing them — useful for metrics, auditing, and cost tracking.
 */
@Component
public class CostGuardProvider implements AgentHookProvider {

  private static final Logger log = LoggerFactory.getLogger(CostGuardProvider.class);

  private final Map<String, AtomicLong> callCounts = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> totalDurations = new ConcurrentHashMap<>();

  @Override
  public void registerHooks(AgentHookRegistry registry) {
    registry.on(
        AfterToolCall.class,
        20,
        event -> {
          callCounts.computeIfAbsent(event.toolName(), k -> new AtomicLong(0)).incrementAndGet();
          totalDurations
              .computeIfAbsent(event.toolName(), k -> new AtomicLong(0))
              .addAndGet(event.duration().toMillis());

          log.info(
              "[Hook] Cost tracker: {} — {} calls, {}ms total",
              event.toolName(),
              callCounts.get(event.toolName()).get(),
              totalDurations.get(event.toolName()).get());

          return HookDecision.proceed();
        });
  }

  /** Returns a snapshot of per-tool call counts. */
  public Map<String, Long> getCallCounts() {
    Map<String, Long> snapshot = new ConcurrentHashMap<>();
    callCounts.forEach((k, v) -> snapshot.put(k, v.get()));
    return snapshot;
  }

  /** Returns a snapshot of per-tool total durations. */
  public Map<String, Duration> getTotalDurations() {
    Map<String, Duration> snapshot = new ConcurrentHashMap<>();
    totalDurations.forEach((k, v) -> snapshot.put(k, Duration.ofMillis(v.get())));
    return snapshot;
  }
}
