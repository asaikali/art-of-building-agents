# Step 15 — Analyze with Trajectory Classification

"Why did it fail that way?" — classify tool calls into semantic states, detect loops and hotspots. This is the stage nobody else does.

## What's new vs Step 14

- **TrajectoryClassifier** — maps tool calls to semantic states (SEARCH, CHECK_BUDGET, CHECK_DIETARY, BOOK, CLARIFY)
- **Loop detection** — identifies when the agent calls the same tool consecutively
- **Hotspot analysis** — finds the most-visited state and self-loop count
- **Efficiency score** — productive states vs total states
- **Combined view** — state panel shows trajectory analysis alongside the verdict

## Semantic State Mapping

| Tool Call | Semantic State |
|-----------|---------------|
| `searchRestaurants` | SEARCH |
| `checkExpensePolicy` | CHECK_BUDGET |
| `checkDietaryOptions` | CHECK_DIETARY |
| `bookTable` | BOOK |
| (no tool) | CLARIFY |

## Key files

| File | Purpose |
|------|---------|
| `TrajectoryClassifier.java` | Classifies tool calls, computes metrics, detects loops |
| `TrajectoryHandler.java` | Runs Jarvis with recording, shows trajectory + verdict in state panel |
| `ToolCallTracker.java` | Thread-local tracker for tool call names |
| `RestaurantTools.java` | Same 4 tools, each call recorded via ToolCallTracker |
| `*Judge.java` | Same 3-tier judges as Step 14 |

## Run it

```bash
cd agents/15-trajectory
OPENAI_API_KEY=sk-... ../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to observe

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

## Why this matters

Recording + classification = trajectory fingerprint. Now you can see WHERE the agent wastes time, not just WHETHER it passed. This is the data that drives targeted improvements.

## New dependencies

Same as Step 14 (`agent-judge-core`, `agent-judge-llm`, `workflow-core`).

## Next step

Step 16 closes the loop — a quality gate that makes the agent self-correct on failure.
