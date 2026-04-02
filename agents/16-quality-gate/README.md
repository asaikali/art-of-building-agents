# Step 16 — Improve with Quality Gate

"What knowledge fixes the behavioral gaps?" — close the loop. The agent generates, a judge evaluates, and on failure, feedback drives revision. This is the stage that makes agents get better.

## What's new vs Step 14-15

- **Workflow DSL gate** — `JudgeGate` wraps a CascadedJury as a workflow gate
- **Self-correction loop** — on failure, a reflector transforms the verdict into feedback, and the agent revises
- **Max retries** — capped at 2 revision attempts to prevent infinite loops
- **Deliberate failure** — default goal triggers expense policy violation, showing the correction in action

## Architecture

```java
Workflow.<String, String>define("jarvis-with-quality-gate")
    .step(recommend)              // AI: generate recommendation
    .gate(judgeGate)              // Evaluate with CascadedJury
        .onPass(formatResult)     // All checks passed -> done
        .onFail(recommend)        // Failed -> revise with feedback
        .withReflector(reflector) // Verdict -> constructive guidance
        .maxRetries(2)            // Cap revision attempts
    .end()
    .run(userRequest);
```

## Demo Scenario

Default goal: "Find a restaurant in Paral-lel for a team lunch with 6 people."

```
Attempt 1: Tickets Bar (EUR 75/person) -> FAIL: expense policy (EUR 50 limit)
Feedback: "Price EUR 75 exceeds EUR 50 limit. Recommend a cheaper option."
Attempt 2: Cerveceria Catalana (EUR 28/person) -> PASS
```

## Key files

| File | Purpose |
|------|---------|
| `QualityGateWorkflow.java` | Workflow with gate: recommend -> judge -> reflect -> revise |
| `QualityGateApplication.java` | CommandLineRunner with deliberate-failure demo |
| `ExpensePolicyJudge.java` | Deterministic judge for the gate |
| `RestaurantData.java` | Shared restaurant data and formatting |

## Run it

```bash
cd agents/16-quality-gate
OPENAI_API_KEY=sk-... ../../mvnw spring-boot:run
```

Custom goal:
```bash
OPENAI_API_KEY=sk-... ../../mvnw spring-boot:run -Dspring-boot.run.arguments="Find a quiet restaurant in Eixample for 4 people"
```

## Why this matters

The loop from Eval back to Improve is what makes agents get better. The gate pattern automates it — generate, judge, reflect, revise. Each cycle tightens the feedback loop until the output passes all checks.

## New dependencies

| Artifact | Purpose |
|----------|---------|
| `io.github.markpollack:workflow-flows` | Workflow DSL + Gate + JudgeGate |
| `org.springaicommunity:agent-judge-core` | Judge, CascadedJury for the gate |
| `org.springaicommunity:agent-judge-llm` | LLM judge support |

Versions managed by `agentworks-bom` in the parent POM.

## Workshop Lifecycle Complete

```
Step 01-12: Build -------------- "Can I create an agent?"
Step 13:    Build (structured) - "Can I control what the LLM does vs doesn't?"
Step 14:    Eval --------------- "Does my agent work?"
Step 15:    Analyze ------------ "Why did it fail that way?"
Step 16:    Improve ------------ "What fixes the behavioral gaps?"
                                  ^
                           This is where ROI compounds
```
