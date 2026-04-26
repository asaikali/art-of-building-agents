# Meal Agent

An end-to-end agent that helps a user plan a business meal — aligning intent,
checking constraints, picking a restaurant, and guiding the final decision.

Every numbered module under this directory is a complete, standalone Spring Boot
app that implements the same agent.

**Modules 01-04 walk through the capabilities a real agent needs**, in the order
you'd build them:

1. **Understand and align with the user's intent** — turn what the user types into
   a confirmed, structured statement of what they want. *Extract date, time, party
   size, budget, and dietary needs from a free-form chat.*
2. **Check whether a candidate meets the requirements** — given a single option,
   evaluate it against each requirement, using code where the answer is mechanical
   and the model where it isn't. *Check a restaurant against budget, noise, travel
   time, dietary fit, and venue suitability.*
3. **Discover and evaluate options autonomously** — let the agent search through
   options and produce a shortlist on its own, rather than being walked through
   every step. *Search the restaurant catalog and evaluate each candidate against
   the user's constraints.*
4. **Help the user reach a decision** — answer questions, compare options, and
   recognize when they've chosen one or want to start over. *Compare restaurants,
   look up a menu, book one, or restart with relaxed requirements.*

The same shape applies in other domains — a vacation planner, a hiring shortlist,
a vendor selection — only the candidates and the requirements change.

Spring AI features are the vehicle: each module uses whichever feature best
demonstrates the capability being taught.

## What's in this directory

- `01-intent-alignment/` … `04-decision-support/` — the four progressive modules
- `restaurant-data/` — fake restaurant data, menus, travel-time matrix, and
  availability service used by every module from 02 onward

## How this fits together

```
┌──────────────────────────┐         ┌──────────────────────────┐         ┌──────────────────────────┐
│ scaffold/inspector       │  HTTP   │ scaffold/agent-core      │  calls  │ meal-agent handler       │
│                          │  + SSE  │                          │  the    │                          │
│ Browser UI for testing   │ ──────► │ Spring Boot backend for  │ handler │ The meal-planning logic. │
│ the agent. Chat with it  │         │ the UI. Holds sessions   │ ──────► │ Extracts requirements,   │
│ and inspect its internal │ ◄────── │ and dispatches messages  │ ◄────── │ checks restaurants,      │
│ state and event stream   │  reply  │ to a pluggable handler   │  reply  │ plans the meal, and      │
│ live.                    │ + state │ via strategy interfaces. │ + state │ guides the decision.     │
└──────────────────────────┘         └──────────────────────────┘         └──────────────────────────┘
```

**scaffold** is the reusable platform — agent runtime + observation UI. It knows
nothing about meals.

**meal-agent** is one agent built on that platform. Each numbered module wires a
`JarvisAgentHandler` into agent-core; that handler is where all the
agent-specific work lives.

For the platform itself, see [`../scaffold/inspector/README.md`](../scaffold/inspector/README.md)
and [`../scaffold/agent-core/`](../scaffold/agent-core/).

## Modules

| Module | Spring AI features used |
|--------|-------------------------|
| `01-intent-alignment` | `ChatClient`, structured output (`.entity()`) |
| `02-constraint-checking` | adds deterministic/hybrid/LLM-as-judge checks |
| `03-restaurant-planning` | adds `@Tool` + `ToolCallAdvisor` |
| `04-decision-support` | adds structured output for action routing + workflow phases |

Each module is a standalone Spring Boot app and contains all the code from
earlier modules plus whatever the new module adds. Open any module and you have
the full picture up to that point.

## How to approach them

1. **Start with 01.** Read the alignment pipeline, run it, and watch how the
   extractor, assessor, and composer take turns in the inspector.
2. **Move to 02.** See how constraint checks are organized by type (deterministic,
   hybrid, LLM-as-judge). Run the unit tests. Notice what's checked with code vs
   what needs a model.
3. **Move to 03.** See how the checks become tools the model can call. Trace a
   planning session in the inspector — watch the model search for restaurants,
   evaluate candidates, and produce a shortlist.
4. **Move to 04.** See how the agent stays in a conversational phase after
   planning. Compare restaurants, ask follow-ups, pick one. Notice how
   structured output routes the user's intent to the right handler action.
5. **Continue with 05+** to see the agent rebuilt with more advanced Spring AI
   features.

## Run a module

```bash
cd meal-agent/<module>
../../mvnw spring-boot:run
```

Then open <http://localhost:8080> — that's the inspector UI. Chat on the left,
agent state and events on the right.

Any module is runnable on its own. Later modules contain everything earlier
modules have, so running 04 is the way to see the full agent end-to-end.

## Run the tests

```bash
cd meal-agent/<module>

# Unit tests (no API key needed)
../../mvnw test

# Integration tests (requires OPENAI_API_KEY)
../../mvnw test -Dgroups=integration
```

Modules 03 and 04 also include a named end-to-end walkthrough scenario
(`VegetarianDinnerWalkthrough`) — see those module READMEs for what to watch.
