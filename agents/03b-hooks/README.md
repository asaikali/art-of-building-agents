# Step 03b — Hooks

Programmatic tool-call interception with [agent-hooks](https://github.com/markpollack/agent-hooks).

## What's new vs Step 03

In Step 03 (guardrails), we told the agent via the system prompt: *"Never recommend a restaurant without checking expense policy first."* The agent might ignore that rule.

In this step, we **enforce it with a hook**. If the agent tries to call `bookTable` before calling `checkExpensePolicy`, the hook **blocks the call** — the tool is never invoked, and the agent receives the block reason as the tool result.

## Three hook providers

| Provider | Hook type | What it does |
|----------|-----------|-------------|
| `ToolCallLoggingProvider` | Observe | Logs every tool call with timing |
| `ExpensePolicyProvider` | Steer | Blocks `bookTable` if expense policy wasn't checked |
| `CostGuardProvider` | Track | Accumulates per-tool call counts and durations |

## How it works

```java
// Fresh HookContext per invocation — isolates tool call history per turn
HookContext hookContext = new HookContext();
ToolCallbackProvider hookedTools = HookedTools.wrap(registry, hookContext, restaurantTools);

chatClient.prompt()
    .messages(history)
    .toolCallbacks(hookedTools)  // hooked tools with per-invocation context
    .call();
```

The `ExpensePolicyProvider` is the key teaching moment:

```java
registry.onTool("bookTable", BeforeToolCall.class, 20, event -> {
    boolean policyChecked = event.context().history().stream()
        .anyMatch(r -> r.toolName().equals("checkExpensePolicy"));
    if (!policyChecked) {
        return HookDecision.block("Expense policy must be checked before booking.");
    }
    return HookDecision.proceed();
});
```

## Run

```bash
./mvnw spring-boot:run -pl agents/03b-hooks
```

Open the Inspector at http://localhost:8080. The state panel shows hook activity — tool call counts, blocked bookings, and per-tool timing.

## Try it

Send: *"I need a restaurant in Eixample for 4 people, budget is 30 euros per person, one guest is vegetarian"*

Watch the logs — you'll see all three hooks fire for each tool call. The state panel shows cumulative hook activity.

To test the expense policy block, send: *"Book a table at Tickets Bar for 4 people tomorrow at 8pm"*

If the agent tries to call `bookTable` before calling `checkExpensePolicy`, the hook blocks it and tells the agent to check expense policy first. The agent then self-corrects.

**Tip:** A well-behaved model (e.g., GPT-4o) usually follows the system prompt and checks expense policy before booking, so the block rarely fires. To reliably demonstrate the block, temporarily remove the "Use checkExpensePolicy BEFORE recommending any restaurant" line from the system prompt — the hook still enforces the rule even when the prompt doesn't mention it. That's the whole point: prompt suggests, hook enforces.

## Design: per-invocation HookContext

The `HookContext` (which tracks tool call history) is created **fresh for each `onMessage` call**. This follows the universal pattern across agent frameworks (Strands, Spring AI, Claude SDK): registry is long-lived, context is per-invocation.

Within a single turn, tool calls accumulate in the context — so `checkExpensePolicy` → `bookTable` works. Between turns, the context resets — every booking attempt must re-verify expense policy in the same turn. The registry (singleton) and hook providers (singletons) persist across all invocations, so cumulative metrics in the state panel keep accumulating.
