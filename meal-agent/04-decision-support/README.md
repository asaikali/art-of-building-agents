# 04 Decision Support

Let the user explore the shortlist, compare restaurants, ask questions, and pick one.

> **Builds on:** 03 (restaurant planning).
> **Adds:** supporting the user's decision after the shortlist — answering,
> comparing, picking, restarting — via workflow phase tracking and
> structured-output action routing.

## What this module teaches

- **Workflow phases** — the agent tracks whether it's in alignment or exploring options.
  Not every message restarts the pipeline.
- **Structured output for routing** — the model returns a typed `DecisionSupportResponse`
  with an `action` field ("answer", "restart", "selected") that the handler uses to decide
  what happens next.
- **Cached context + on-demand tools** — the shortlist is included in the user prompt on
  every turn for quick comparison questions. Tools are available for deeper lookups
  (menus, details).
- **Graceful state transitions** — "restart" goes back to alignment, "selected" logs a
  booking event and resets for a new meal.

## Architecture

```
decisionsupport/
  DecisionSupport.java          — ChatClient with shortlist context + tools,
                                   returns structured DecisionSupportResponse
  DecisionSupportResponse.java  — record: action ("answer"/"restart"/"selected") + reply

agent/
  WorkflowPhase.java            — ALIGNMENT, EXPLORING_OPTIONS
  JarvisAgentContext.java       — tracks phase, alignment status, shortlist
  JarvisAgentHandler.java       — routes by phase:
                                    ALIGNMENT → alignment pipeline → planning
                                    EXPLORING_OPTIONS → decision support
```

The decision support flow:
1. Planning produces shortlist → phase moves to `EXPLORING_OPTIONS`
2. User asks "compare Canoe vs The Chase" → `DecisionSupport.ask()` answers from context
3. User asks "what's on the menu at Canoe?" → model calls `getRestaurantMenu` tool
4. User says "book Canoe" → model returns `action: "selected"` → handler logs event, resets
5. User says "try a higher budget" → model returns `action: "restart"` → back to alignment

Trace `VegetarianDinnerWalkthrough` for a full end-to-end session covering alignment,
planning, and decision-support phases.

## Key design decisions

- **The model classifies intent via structured output.** Instead of parsing keywords from
  free text, the model returns a typed action. Spring AI's `.entity()` handles deserialization.
- **Tools are shared with planning.** `PlanningTools` is reused — same tools for both
  phases. The `getRestaurantMenu` tool was added specifically for decision support.
- **Booking is an event, not a module.** When the user selects a restaurant, the handler
  logs a `restaurant-booked` event and resets. No separate booking phase needed for the
  workshop.

## Running and testing

See [meal-agent/README.md](../README.md#run-a-module) for run and test commands.
