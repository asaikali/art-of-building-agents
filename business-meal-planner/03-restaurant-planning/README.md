# 03 Restaurant Planning

Use Spring AI tool calling to search for restaurants and evaluate them against constraints.

## What this module teaches

- **Tool calling with Spring AI** — register `@Tool` methods, let the model decide which
  to call and when
- **Availability-first planning** — cheap deterministic search before expensive LLM checks
- **Agentic loop** — one `ChatClient.call()` with tools registered. Spring AI handles the
  tool execution loop. The system prompt tells the model the strategy.
- **Prompt as strategy** — the system prompt describes the planning approach in plain
  English. No tool names in the prompt — the model matches actions to tools from their
  `@Tool` descriptions.

## Architecture

```
planning/
  PlanningTools.java       — @Tool methods: findAvailableRestaurants, getRestaurantDetails,
                             getRestaurantMenu, checkRestaurantCandidate
  RestaurantPlanner.java   — builds ChatClient with tools, makes single planning call

agent/
  JarvisAgentHandler.java  — after alignment confirms, triggers planning immediately
```

The planning flow:
1. Alignment confirms requirements
2. Handler calls `RestaurantPlanner.plan(confirmedRequirements)`
3. Model calls `findAvailableRestaurants` → gets candidate list
4. Model calls `checkRestaurantCandidate` on promising candidates → gets check results
5. Model produces a shortlist or explains what failed

## Key design decisions

- **Tools accept name or ID.** The `resolveRestaurantId` helper tries exact ID match first,
  then falls back to name matching. This prevents failures when the model passes display
  names instead of IDs.
- **No while loop.** Spring AI's `ToolCallAdvisor` handles the multi-turn tool calling
  automatically. The Java code makes one call.
- **Tone rules in the prompt.** The model writes like a concierge — no PASS/FAIL jargon
  in the user-facing response. Failed restaurants are omitted entirely.

## Run it

```bash
cd business-meal-planner/03-restaurant-planning
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## Tests

```bash
# Unit tests (no API key needed)
../../mvnw test

# Integration walkthrough (requires API key)
../../mvnw test -Dgroups=integration -Dtest=VegetarianDinnerWalkthrough
```
