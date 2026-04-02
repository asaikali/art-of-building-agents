# Step 09 — Sub-Agent Delegation

Jarvis no longer calls restaurant tools directly. Instead, it delegates research to a specialized sub-agent that runs in its own context with its own system prompt and tools.

## What's new vs Steps 04-08

- **`ResearcherTool`** — a `@Tool` method that creates a fresh `ChatClient` with restaurant tools and a research-optimized prompt
- **Sub-agent isolation** — the researcher runs in its own context window, completely independent from Jarvis
- **Coordinator pattern** — Jarvis coordinates and communicates with the user; the researcher is optimized for thorough search and verification

## Key files

| File | Purpose |
|------|---------|
| `SubagentHandler.java` | Jarvis coordinator — only tool is `delegateToResearcher` |
| `ResearcherTool.java` | Sub-agent dispatcher — creates a fresh ChatClient per research request |
| `RestaurantTools.java` | Same 4 tools, but only available to the sub-agent |

## Run it

```bash
cd agents/09-subagent
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## How it works

```
User: "Find a restaurant in Eixample, 30€/person, vegetarian"
  ↓
Jarvis (main agent):
  - Tools: delegateToResearcher
  - Calls delegateToResearcher("Eixample, 30€/person, 4 people, vegetarian")
      ↓
      Researcher (sub-agent):
        - Own ChatClient, own system prompt, own context
        - Tools: searchRestaurants, checkExpensePolicy, checkDietaryOptions, bookTable
        - Runs full agent loop: search → verify expense → check dietary → filter
        - Returns: "Teresa Carles (€22, vegetarian, Eixample) ✓"
      ↓
  - Presents researcher's findings to the user
```

## What to observe

- The **State panel** shows "Jarvis Tools | delegateToResearcher" — Jarvis only has one tool
- The **Events panel** shows the delegation happening
- Response may take slightly longer (two LLM call chains: Jarvis → Researcher)

## Why this matters

- **Separation of concerns** — each agent is optimized for its role
- **Context isolation** — the sub-agent's context doesn't pollute Jarvis's conversation
- **Composability** — you can add more sub-agents (venue researcher, transport planner, etc.)
- **Cost efficiency** — sub-agents can use cheaper models for routine tasks

## The sub-agent is just another ChatClient

The key insight: a sub-agent is simply another `ChatClient.builder(chatModel).defaultSystem(...).defaultTools(...).build()` call. No special framework needed — it's the same API you've been using since Step 02, just composed differently.

For more sophisticated patterns (background tasks, task queues, skill injection), see `TaskTool` from `spring-ai-agent-utils`.

## Next step

Step 10 will turn the expense policy checker into a separate A2A (Agent-to-Agent) agent, discoverable via Agent Card.
