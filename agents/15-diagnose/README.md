# Step 15 — Diagnose

Record, evaluate, and analyze agent behavior in one module. Three feedback signals in one handler.

## What's here

This step combines the capabilities from Steps 05 (Journal) and 14 (Judge) with trajectory classification:

- **Journal** — records every loop event (turns, tokens, cost) to JSONL via `JournalLoopListener`
- **Judge** — 3-tier `CascadedJury` evaluates output correctness (T0: expense, T1: dietary, T2: LLM quality)
- **Trajectory** — classifies tool calls into semantic states, detects loops and hotspots, computes efficiency

## Key files

| File | Purpose |
|------|---------|
| `DiagnoseHandler.java` | Runs Jarvis, records to journal, captures trajectory, evaluates with jury |
| `JournalLoopListener.java` | Bridges AgentLoopAdvisor lifecycle events into journal events |
| `TrajectoryClassifier.java` | Maps tool calls to semantic states, computes metrics |
| `ToolCallTracker.java` | Thread-local tracker for tool call names |
| `RestaurantTools.java` | Same 4 tools, each call recorded via ToolCallTracker |
| `*Judge.java` | 3-tier judges: ExpensePolicy (T0), DietaryCompliance (T1), RecommendationQuality (T2) |

## Semantic State Mapping

| Tool Call | Semantic State |
|-----------|---------------|
| `searchRestaurants` | SEARCH |
| `checkExpensePolicy` | CHECK_BUDGET |
| `checkDietaryOptions` | CHECK_DIETARY |
| `bookTable` | BOOK |
| (no tool) | CLARIFY |

## Run it

```bash
source ~/.env   # needs OPENAI_API_KEY
cd agents/15-diagnose
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to observe

The Inspector state panel shows three sections:

1. **Status** — turn count, journal location
2. **Trajectory Analysis** — semantic state sequence, loop count, efficiency %
3. **Verdict** — per-judge PASS/FAIL with reasoning

Good trajectory (efficient agent):
```
Sequence: SEARCH -> CHECK_BUDGET -> CHECK_DIETARY -> BOOK
States: 4 | Loops: 0 | Efficiency: 100%
```

Bad trajectory (looping agent):
```
Sequence: SEARCH -> SEARCH -> SEARCH -> CHECK_BUDGET -> BOOK
States: 5 | Loops: 2 | Efficiency: 60%
Warning: Hotspot: SEARCH (3 visits, 2 self-loops)
```

Journal output written to `.agent-journal/` — JSONL files with per-turn events.

## Covered in slide deck

This module is presented in `slidev-2/` under the **Diagnose** section.

## Next step

Step 16 closes the loop — a quality gate that makes the agent self-correct on failure.
