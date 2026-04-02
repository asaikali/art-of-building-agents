# Step 14 — Eval with CascadedJury

"Does the agent work?" — evaluate Jarvis output with a 3-tier cascaded jury. Deterministic judges run first (fast, free). LLM judges only fire if deterministic tiers pass.

## What's new vs Step 09

- **agent-judge** — `agent-judge-core` + `agent-judge-llm` provide Judge, Jury, CascadedJury, Verdict
- **3-tier cascade** — deterministic fail-fast, then structural check, then LLM quality evaluation
- **Verdict in state panel** — Inspector shows per-judge status and reasoning after each conversation

## Architecture: 3-Tier Cascade

| Tier | Judge | Type | Policy | What It Checks |
|------|-------|------|--------|---------------|
| T0 | `ExpensePolicyJudge` | Deterministic | REJECT_ON_ANY_FAIL | Price <= EUR 50/person? |
| T1 | `DietaryComplianceJudge` | Deterministic | ACCEPT_ON_ALL_PASS | Restaurant has stated dietary options? |
| T2 | `RecommendationQualityJudge` | LLM | FINAL_TIER | Recommendation well-reasoned and helpful? |

If T0 fails, the cascade stops immediately — no tokens spent on T2.

## Key files

| File | Purpose |
|------|---------|
| `JudgeHandler.java` | Runs Jarvis, then evaluates with CascadedJury, shows verdict in state panel |
| `ExpensePolicyJudge.java` | T0: deterministic price check against RESTAURANTS data |
| `DietaryComplianceJudge.java` | T1: deterministic dietary compliance check |
| `RecommendationQualityJudge.java` | T2: LLM judge for recommendation quality |
| `RestaurantTools.java` | Same 4 tools as previous steps |
| `JudgeApplication.java` | Spring Boot entry point |

## Run it

```bash
cd agents/14-judge
OPENAI_API_KEY=sk-... ../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to try

Ask: "I need a restaurant in Eixample for 4 people, budget is 30 euros per person, one guest is vegetarian"

The state panel will show the verdict breakdown:

```
| Judge | Status | Reasoning |
|-------|--------|-----------|
| ExpensePolicy | PASS | Price EUR 22 within EUR 50 limit |
| DietaryCompliance | PASS | Teresa Carles has vegetarian options |
| RecommendationQuality | PASS | Clear, well-reasoned |
```

Try a prompt that triggers failure: "Find a restaurant in Paral-lel" — Tickets Bar at EUR 75 fails expense policy.

## Why this matters

The same deterministic tools that power the agent (`checkExpensePolicy`) now power the judges. Build once, measure with the same logic. The cascade pattern means you only spend LLM tokens when the cheap checks pass.

## New dependencies

| Artifact | Purpose |
|----------|---------|
| `org.springaicommunity:agent-judge-core` | Judge, Jury, CascadedJury, Verdict |
| `org.springaicommunity:agent-judge-llm` | LLMJudge base class |

Versions managed by `agentworks-bom` in the parent POM.

## Next step

Step 15 adds trajectory analysis — classifying tool call sequences to find loops and hotspots.
