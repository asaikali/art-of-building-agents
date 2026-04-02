# Step 13 — Workflow DSL (Stripe Pattern)

Not everything should be a free-form agent loop. The "context → AI → validate" pattern sandwiches the LLM between deterministic steps. Stripe processes 1,300 PRs/week this way.

## What's new vs Step 12

- **Workflow DSL** — `workflow-flows` provides `Workflow.define()` with typed steps
- **Three-step sandwich** — deterministic filtering → LLM reasoning → deterministic validation
- **Zero wasted tokens** — the LLM only sees pre-filtered candidates, validation is instant

## Architecture

```
Step 1 (deterministic): gather-context
  Parse request → extract constraints → filter restaurants → 0 tokens

Step 2 (AI): recommend
  LLM picks best option from filtered candidates → reasoning only

Step 3 (deterministic): validate
  Verify recommendation is in candidate list, fits budget → 0 tokens
```

```java
Workflow.<DinnerRequest, DinnerResult>define("dinner-planning")
    .step(gatherContext)       // deterministic
    .step(recommend)           // AI
    .step(validate)            // deterministic
    .run(request);
```

## Key files

| File | Purpose |
|------|---------|
| `DinnerPlanningWorkflow.java` | Three-step workflow: gather → recommend → validate |
| `WorkflowApplication.java` | CommandLineRunner that builds and runs the workflow |
| `DinnerRequest.java` | Input record: neighborhood, cuisine, party size, budget, dietary |
| `DinnerResult.java` | Output record: candidates, recommendation, validation status |

## Run it

```bash
cd agents/13-workflow
OPENAI_API_KEY=sk-... ../../mvnw spring-boot:run
```

## What to observe

- **Candidates** are filtered deterministically — no LLM tokens spent on restaurants that don't fit
- **Validation** catches hallucinations — if the LLM recommends a restaurant not in the candidate list, validation fails
- **Timing** shows the AI step takes most of the time; deterministic steps are near-instant

## Why this matters

The LLM does ONLY what it's good at (reasoning over a curated set of options). Deterministic steps handle filtering and validation — zero tokens, guaranteed correct, instant. This is the pattern that scales to production.

## New dependencies

| Artifact | Purpose |
|----------|---------|
| `io.github.markpollack:workflow-flows` | Workflow DSL (Step, Workflow.define()) |

Version managed by `agentworks-bom` in the parent POM.

## Next step

Step 14 adds evaluation — a CascadedJury of deterministic + LLM judges that score agent output.
