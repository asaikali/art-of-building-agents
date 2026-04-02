# Step 07 — Memory

Same agent as Steps 04-06, but now the agent remembers context across conversations. Accumulated knowledge is persisted to disk and automatically compacted when the token budget is exceeded.

## What's new vs Steps 04-06

- **`FileSystemMemoryStore`** — append-only memory stored in `.agent-memory/` (iterations + compacted patterns)
- **`CompactionMemoryAdvisor`** — Spring AI advisor that injects memory before each call and appends learnings after each response
- **LLM-powered compaction** — when uncompacted entries exceed the token budget, gpt-4o-mini summarizes them into dense patterns

## Key files

| File | Purpose |
|------|---------|
| `MemoryHandler.java` | Wires `CompactionMemoryAdvisor` + `AgentLoopAdvisor` into the ChatClient |
| `RestaurantTools.java` | Same 4 tools as Steps 03-06 |

## Run it

```bash
cd agents/07-memory
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to try

1. Ask for a restaurant recommendation (same prompts as Steps 03-06)
2. Have a second conversation — the agent now has memory of the first
3. Check the memory store:

```bash
# See raw memory entries
ls .agent-memory/iterations/

# See compacted summaries (after several conversations)
ls .agent-memory/patterns/

# View the memory index
cat .agent-memory/_index.json | python3 -m json.tool
```

## What to observe

- The **State panel** shows "Memory | .agent-memory/ (compaction via gpt-4o-mini)"
- On the first conversation, memory is empty — behavior is identical to Step 04
- On subsequent conversations, the advisor injects accumulated knowledge into the system prompt
- After enough entries accumulate, compaction kicks in — old entries are summarized and stored in `patterns/`

## How CompactionMemoryAdvisor works

```
Before each LLM call:
  1. Retrieve memory within token budget (4096 tokens)
  2. Prioritize compacted patterns (dense knowledge) over raw entries
  3. Inject into system prompt under MEMORY: section

After each LLM response:
  1. Append the response to the memory store
  2. If uncompacted entries exceed budget × compactionRatio → trigger compaction
  3. Compaction: gpt-4o-mini summarizes old entries into patterns/compact-*.md
```

## Why this matters

Without memory, every conversation starts from zero. With memory:
- The agent learns user preferences over time
- Past bookings and constraints are remembered
- Repeated mistakes are avoided
- The token budget + compaction keeps memory from growing unbounded

## New dependencies

| Artifact | Purpose |
|----------|---------|
| `io.github.markpollack:memory-core` | FileSystemMemoryStore, MemoryCompactor |
| `io.github.markpollack:memory-advisor` | CompactionMemoryAdvisor (Spring AI BaseAdvisor) |

Both versions managed by `agentworks-bom` in the parent POM.

## Next step

Step 08 will add human-in-the-loop — the agent asks the user for clarification instead of guessing.
