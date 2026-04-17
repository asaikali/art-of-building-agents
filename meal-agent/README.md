# Meal Agent

An end-to-end agent that helps a user plan a business meal — aligning intent,
checking constraints, picking a restaurant, and guiding the final decision.

Every numbered module under this directory is a complete, standalone Spring Boot
app that implements the meal agent. What differs between modules is **which Spring
AI features the agent is built with**. The numbered sequence walks from the most
basic implementation (just `ChatClient` and structured output) to progressively
richer variations (tool calling, memory, MCP, human-in-the-loop, sub-agents, and
so on).

## Modules

| Module | Spring AI features used |
|--------|-------------------------|
| `01-intent-alignment` | `ChatClient`, structured output (`.entity()`) |
| `02-constraint-checking` | adds deterministic/hybrid/LLM-as-judge checks |
| `03-restaurant-planning` | adds `@Tool` + `ToolCallingAdvisor` |
| `04-decision-support` | adds structured output for action routing + workflow phases |

Later modules (05+) follow the convention `NN-with-{feature}` — e.g.
`05-with-memory`, `06-with-mcp` — each rebuilding the agent using a different
Spring AI feature to show that feature in a real agent context.

Each module is a standalone Spring Boot app and contains all the code from
earlier modules plus whatever the new module adds. Students can open any module
and see the full picture up to that point.

## How to approach them

1. **Start with 01.** Read the alignment pipeline, run it in the inspector, understand how
   the extractor, assessor, and composer work together.
2. **Move to 02.** See how constraint checks are organized by type (deterministic, hybrid,
   LLM-as-judge). Run the unit tests. Notice what's checked with code vs what needs a model.
3. **Move to 03.** See how the checks become tools the model can call. Trace a planning
   session in the inspector — watch the model search for restaurants, evaluate candidates,
   and produce a shortlist.
4. **Move to 04.** See how the agent stays in a conversational phase after planning. Ask
   questions about the shortlist, compare restaurants, pick one. Notice how structured output
   routes the user's intent to the right handler action.
5. **Continue with 05+** to see the agent rebuilt with more advanced Spring AI features.

## Run any module

```bash
cd meal-agent/01-intent-alignment
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## Shared infrastructure

- **`scaffold/agent-core`** — session management, chat, state, events, SSE streaming
- **`meal-agent/restaurant-data`** — fake restaurant data, menus, travel time matrix,
  availability service (lives here because only meal-agent modules use it)
- **`scaffold/inspector/`** — Vue 3 frontend for observing agent behavior in real time
