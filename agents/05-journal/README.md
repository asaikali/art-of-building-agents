# Step 05 — Journal Recording

Same agent as Step 04, but now every interaction is recorded to an append-only JSONL file. This is the seam between "build" and "measure."

## What's new vs Step 04

- **agent-journal** — `journal-core` provides structured recording of agent interactions
- **`JournalLoopListener`** — bridges `AgentLoopAdvisor` events (turn started, turn completed, loop finished) into journal events
- **JSONL output** — after each interaction, check `.agent-journal/` for a complete record of what happened: turns, tokens, cost, termination reason

## Key files

| File | Purpose |
|------|---------|
| `JournalHandler.java` | Creates a journal `Run` per interaction, wires `JournalLoopListener` into the advisor |
| `JournalLoopListener.java` | Implements `AgentLoopListener`, logs events to the journal `Run` |
| `RestaurantTools.java` | Same 4 tools as Steps 03-04 |

## Run it

```bash
cd agents/05-journal
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to try

Same prompts as Steps 03-04. After a conversation:

```bash
# See the journal output
find .agent-journal -name "*.jsonl" | head -1 | xargs cat | python3 -m json.tool --no-ensure-ascii
```

You'll see events like:
- `loop_started` — with the user's message
- `turn_started` / `turn_completed` — one per LLM call in the agent loop
- `loop_completed` — with total turns, tokens, cost, and termination reason
- `assistant_reply` — the final response

## What to observe

- The **State panel** now shows "Journal | .agent-journal/ (JSONL)"
- The JSONL file is append-only — every run gets its own directory under `.agent-journal/experiments/`
- The `loop_completed` event includes structured metrics: `turnsCompleted`, `totalTokens`, `estimatedCost`

## Why this matters

Without recording, you can't measure. Without measurement, you can't improve. The journal data feeds:
- **Trajectory analysis** — which tool sequences lead to success?
- **Heatmaps** — where does the agent spend time?
- **Judge scoring** — did the agent make correct decisions?

This is the data that turns "I built an agent" into "I can prove my agent works."

## New dependencies

| Artifact | Purpose |
|----------|---------|
| `io.github.markpollack:journal-core` | Structured recording to JSONL |
| `io.github.markpollack:workflow-core` | AgentLoopAdvisor (same as Step 04) |

Both versions managed by `agentworks-bom` in the parent POM.

## Next step

Step 06 will move the restaurant tools to an MCP server — the agent discovers tools dynamically instead of having them compiled in.
