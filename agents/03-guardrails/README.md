# Step 03 — Guardrails

Same Jarvis agent, now with business rule enforcement. Four tools and a constraint-aware system prompt that tells the agent **when** and **how** to use each tool.

## What's new vs Step 02

- **Three new tools**: `checkExpensePolicy`, `checkDietaryOptions`, `bookTable`
- **Constraint-aware system prompt** — explicit rules: "Never recommend a restaurant without checking expense policy first"
- **Deterministic tools** — expense policy (hard limit: 50 EUR/person) and dietary checks apply business rules, not AI judgment

## Key files

| File | Purpose |
|------|---------|
| `GuardrailsHandler.java` | Richer system prompt with constraint rules and tool selection guidance |
| `RestaurantTools.java` | 4 tools: `searchRestaurants`, `checkExpensePolicy`, `checkDietaryOptions`, `bookTable` |

## Run it

```bash
cd agents/03-guardrails
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to try

- "I need a restaurant for 4 people, budget is 30 euros per person, one guest is vegetarian"
  - Should recommend Teresa Carles (22 EUR, vegetarian, in Eixample)
  - Should reject El Nacional (55 EUR — over budget) and Tickets Bar (75 EUR, no vegetarian)
- "Book a table at Can Culleretes for tomorrow at 8pm, 6 people"

## What to observe

- The agent follows a multi-step process: search -> check expense -> check dietary -> recommend
- **Deterministic tools become judges**: the same expense/dietary logic will later evaluate agent quality
- Watch the **Events panel** to see the tool call sequence

## What's still missing

No turn limit. If the agent gets stuck in a loop (searching, checking, re-searching), there's no way to stop it. No cost tracking.

## Next step

[04-turn-limits](../04-turn-limits/) replaces `ToolCallAdvisor` with `AgentLoopAdvisor` for governed execution.
