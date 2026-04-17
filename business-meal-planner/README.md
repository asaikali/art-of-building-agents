# Business Meal Planner

An end-to-end business meal planning agent built from first principles using Spring AI,
`ChatClient`, and tool calling. Each module adds one architectural idea — students work
through them in order, building up a complete agent one concept at a time.

## Modules

| Module | What it teaches |
|--------|----------------|
| `01-intent-alignment` | Structured extraction, confirmation loops, deterministic status transitions |
| `02-constraint-checking` | Deterministic, hybrid, and LLM-as-judge constraint checks |
| `03-restaurant-planning` | Tool calling with Spring AI, agentic search and evaluation |
| `04-decision-support` | Conversational follow-up, structured output for action routing, workflow phases |

Each module is a standalone Spring Boot app. Later modules include the code from earlier
ones — module 04 contains all the code from 01, 02, and 03 plus the new decision support
layer. This means students can open any module and see the full picture up to that point.

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

## Run any module

```bash
cd business-meal-planner/01-intent-alignment
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## Shared infrastructure

- **`scaffold/agent-core`** — session management, chat, state, events, SSE streaming
- **`components/restaurant-data`** — fake restaurant data, menus, travel time matrix,
  availability service
- **`scaffold/inspector/`** — Vue 3 frontend for observing agent behavior in real time
