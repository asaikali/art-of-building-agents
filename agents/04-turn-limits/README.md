# Step 04 — Turn Limits

Same tools, same prompt — but the loop is now governed. This is the first step that uses an AgentWorks library.

## What's new vs Step 03

- **`AgentLoopAdvisor`** replaces `ToolCallAdvisor` — same loop underneath, but adds:
  - **maxTurns(15)**: hard stop after 15 iterations
  - **Stuck detection**: SHA-256 hashing of tool calls, detects 5x consecutive repetition or A-B-A-B alternation
  - **Grace turn**: when max turns is hit, one additional tool-free LLM call lets the agent summarize progress
- **First AgentWorks dependency**: `workflow-core` from the agentworks-bom

## Key files

| File | Purpose |
|------|---------|
| `TurnLimitsHandler.java` | `AgentLoopAdvisor.builder().toolCallingManager(...).maxTurns(15).build()` |
| `RestaurantTools.java` | Same 4 tools as Step 03 |
| `pom.xml` | Adds `io.github.markpollack:workflow-core` (version managed by agentworks-bom) |

## Run it

```bash
cd agents/04-turn-limits
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to try

Same prompts as Step 03 — the behavior is identical for normal conversations. The difference shows up in edge cases:
- Try a vague prompt that causes many tool calls — the agent will stop at 15 turns with a summary
- The stuck detector catches pathological loops before they burn through your API budget

## The dependency boundary

Steps 01-03 were **pure Spring AI** — no dependencies beyond `spring-ai-starter-model-openai`.

This step crosses into **AgentWorks** territory. The `workflow-core` library extends Spring AI's `ToolCallAdvisor` with production-grade loop governance. From here on, each step adds more AgentWorks capabilities.

## Next step

Step 05 will introduce `AgentLoop` (from `workflow-agents`) and `agent-journal` — packaging the agent into a reusable runner with automatic recording of every tool call and LLM interaction.
