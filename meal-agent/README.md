# Meal Agent

An end-to-end agent that helps a user plan a business meal — aligning intent,
finding restaurants, checking constraints, and guiding the final decision.

Every numbered module under this directory is a complete, standalone Spring Boot
app that implements the same agent.

**Modules 01-06 walk through the capabilities a real agent needs**, in the order
you'd build them:

1. **Understand and align with the user's intent** — turn what the user types into
   a confirmed, structured statement of what they want. *Extract date, time, party
   size, budget, and dietary needs from a free-form chat.*
2. **Find available candidates** — give the aligned agent a restaurant search tool.
   *Search by date, time, party size, and optional neighborhood. Finding an available
   restaurant does not establish whether it meets the other requirements.*
3. **Check whether a candidate meets the requirements** — given a single option,
   evaluate it against each requirement, using code where the answer is mechanical
   and the model where it isn't. *Check a restaurant against budget, noise, travel
   time, dietary fit, and venue suitability.*
4. **Combine discovery and evaluation** — let the agent search through
   options and produce a shortlist on its own, rather than being walked through
   every step. *Search the restaurant catalog and evaluate each candidate against
   the user's constraints.*
5. **Help the user reach a decision** — answer questions, compare options, and
   recognize when they've chosen one or want to start over. *Compare restaurants,
   look up a menu, book one, or restart with relaxed requirements.*

6. **Remember user preferences** — carry favourite restaurants across chats using a custom advisor. *Prioritize qualifying favourites while keeping current meal constraints authoritative.*

The same shape applies in other domains — a vacation planner, a hiring shortlist,
a vendor selection — only the candidates and the requirements change.

Spring AI features are the vehicle: each module uses whichever feature best
demonstrates the capability being taught.

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

**scaffold** is the reusable platform — the [agent runtime](../scaffold/agent-core/)
plus the [observation UI](../scaffold/inspector/README.md). It knows nothing about
meals.

**meal-agent** is one agent built on that platform. Each numbered module wires a
`JarvisAgentHandler` into agent-core; that handler is where all the
agent-specific work lives.

## Suggested study order

Alternate a small feature example with its use in the meal agent. Spring AI
experience is not assumed:

| Step | Lesson | Question it answers |
| --- | --- | --- |
| 1 | [Foundations 01: ChatClient](../foundations/01-chat-client/README.md) | How does application code call a model? |
| 2 | [Foundations 02: Structured output](../foundations/02-structured-output/README.md) | How can the application use the model's answer as data? |
| 3 | [Agent 01: Intent alignment](01-intent-alignment/README.md) | How do we understand and confirm what the user wants? |
| 4 | [Foundations 03: Tool calling](../foundations/03-tool-calling/README.md) | How can a model request information from application code? |
| 5 | [Agent 02: Restaurant search](02-restaurant-search/README.md) | How can the agent find available restaurants? |
| 6 | [Agent 03: Constraint checking](03-constraint-checking/README.md) | Are those restaurants actually suitable? |
| 7 | [Agent 04: Restaurant planning](04-restaurant-planning/README.md) | How can the agent search and evaluate candidates itself? |
| 8 | [Agent 05: Decision support](05-decision-support/README.md) | How can it help the user choose? |
| 9 | [Foundations 04: Advisors](../foundations/04-advisor/README.md) | What surrounds a model call and its tool loop? |
| 10 | [Agent 06: User preferences](06-user-preferences/README.md) | How can user memory improve future meals? |

In 02, watch a real search and inspect its limited results in Spy. In 03, keep
that search demo and introduce the checks as separately testable services. In 04,
expose those checks as tools so the agent can use them during planning. In 05,
compare options, ask follow-ups, and select a restaurant.

Every numbered lesson is an independent snapshot with its own implementation,
entry point, and `pom.xml`. They share scaffold and restaurant data; numbered
lessons do not depend on one another.

## Run a module

The meal-agent backend and the inspector UI run as two processes.

With `OPENAI_API_KEY` set, use the mise tasks from the repository root. Each
backend task builds the selected lesson and its shared dependencies before starting:

| Command | Lesson |
| --- | --- |
| `mise run 1:start` | Intent alignment |
| `mise run 2:start` | Restaurant search |
| `mise run 3:start` | Constraint checking |
| `mise run 4:start` | Restaurant planning |
| `mise run 5:start` | Decision support |
| `mise run 6:start` | User preferences |

Run one backend at a time on port 8080. In another terminal, run
`mise run ui:start` to start the inspector on port 5173.

Alternatively, run Maven and npm directly:

Set `OPENAI_API_KEY`. From the repository root, build the selected lesson and its
shared dependencies, then start its backend (02 shown here):

```bash
./mvnw -pl meal-agent/02-restaurant-search -am install -DskipTests
./mvnw -f meal-agent/02-restaurant-search/pom.xml spring-boot:run
```

In another terminal, start the inspector (see
[`../scaffold/inspector/README.md`](../scaffold/inspector/README.md) for setup):

```bash
cd scaffold/inspector
npm run dev
```

Then open <http://localhost:5173>. Chat on the left, agent state and events on
the right. The inspector proxies its API calls to the backend on port 8080.

Any module is runnable on its own. Later modules include the earlier capabilities
and add the next step, so running 06 shows the full agent with user preference memory.

## Run the tests

```bash
# From the repository root, select the lesson you want to test.
# Unit tests (no API key needed)
./mvnw -pl meal-agent/02-restaurant-search -am test

# Integration tests (requires OPENAI_API_KEY)
./mvnw -pl meal-agent/02-restaurant-search -am test -Dgroups=integration -DexcludedGroups=
```

Modules 04 and 05 also include a named end-to-end walkthrough scenario
(`VegetarianDinnerWalkthrough`) — see those module READMEs for what to watch.
