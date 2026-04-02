# Progressive Agent Examples — Build Plan

> **Branch**: `progressive-agent-examples`
> **Status**: Steps 01-08 implemented, compiling on Spring AI 2.0.0-M3 + Spring Boot 4.1.0-M2 + AgentWorks BOM 1.0.3
> **Domain**: Jarvis — business dinner planning agent for Barcelona

## The Idea

Each module in `agents/` is a standalone Spring Boot app that adds exactly ONE concept to the previous step. Participants `cd` into any step and run it independently. The Inspector (agent-core) stays fixed throughout — only the `AgentHandler` implementation evolves.

Every step uses the same Jarvis restaurant domain: search restaurants, check expense policy, verify dietary requirements, book a table.

## Steps

| Step | Module | What It Adds | Library Source | Status |
|------|--------|-------------|---------------|--------|
| 01 | `basic-chatbot` (Adib's) | Bare ChatClient, no tools. Pass-through to the model. | Spring AI | Done |
| 02 | `02-tool-calling` | System prompt + `searchRestaurants` tool + `ToolCallAdvisor` (Spring AI's built-in agent loop). **First real agent.** | Spring AI | Done |
| 03 | `03-guardrails` | Full tool set: search, `checkExpensePolicy`, `checkDietaryOptions`, `bookTable`. Constraint-aware system prompt with tool selection rules. | Spring AI | Done |
| 04 | `04-turn-limits` | Replace `ToolCallAdvisor` with `AgentLoopAdvisor` — max turns, stuck detection, grace turn recovery. **First AgentWorks library.** | **agent-workflow** | Done |
| 05 | `05-journal` | Wire `agent-journal` via `JournalLoopListener` on `AgentLoopAdvisor`. Every turn recorded to JSONL. The seam between "build" and "measure." | **agent-journal** | Done |
| 06 | `06-mcp-server` + `06-mcp-client` | Move restaurant tools to an MCP server (port 8081, Streamable HTTP). Client agent discovers tools dynamically via `SyncMcpToolCallbackProvider`. MCP SDK 1.1.1. | Spring AI MCP | Done |
| 07 | `07-memory` | Add `CompactionMemoryAdvisor` with `FileSystemMemoryStore`. Persistent memory across conversations, LLM-powered compaction via gpt-4o-mini. | **agent-memory** | Done |
| 08 | `08-human-in-the-loop` | Add `AskUserQuestionTool` — agent pauses mid-loop to ask user for clarification. `CompletableFuture` bridge to Inspector web UI. | **spring-ai-agent-utils** | Done |
| 09 | `09-subagent` | Add `TaskTool` — delegate restaurant research to a sub-agent. | spring-ai-agent-utils | TODO |
| 10 | `10-a2a` | Expense policy checker as a separate A2A agent. Jarvis discovers it via `AgentCard` and calls via `@Tool sendMessage()`. | **spring-ai-a2a** | TODO |
| 11 | `11-acp` | Expose Jarvis as an ACP endpoint. `@AcpAgent` + `@Prompt`. Web UI and IDE can talk to Jarvis. | **acp-java** | TODO |
| 12 | `12-wrap-path` | Show `agent-client` wrapping Claude Code / Gemini CLI — same journal, same judges, different build path. | **agent-client** | TODO |

### Dependency Boundary

**Steps 01-03**: Pure Spring AI. No external dependencies beyond spring-ai-starter-model-openai.
**Step 04**: First AgentWorks library enters (agent-workflow for AgentLoopAdvisor).
**Steps 05-12**: Progressive AgentWorks + protocol libraries.

## How Each Step Works

Each step is a self-contained Spring Boot app:

```
agents/02-tool-calling/
├── pom.xml                    (depends on agent-core)
├── src/main/java/.../
│   ├── ToolCallingApplication.java   (Spring Boot main)
│   ├── ToolCallingHandler.java       (implements AgentHandler)
│   └── RestaurantTools.java          (@Tool methods)
└── src/main/resources/
    └── application.yml
```

To run any step:
```bash
cd agents/02-tool-calling
../mvnw spring-boot:run
# Open http://localhost:8080 → Inspector UI
```

## Architecture

```
┌─────────────────────────────────────┐
│  Inspector UI (Vue.js — from agent-core)    │
│  Chat Panel  │  State Viewer  │  Events     │
├──────────────┴────────────────┴─────────────┤
│  agent-core (fixed scaffold)                 │
│  Session, ChatService, EventService,         │
│  StateService, SSE Broadcasting              │
├──────────────────────────────────────────────┤
│  AgentHandler (evolves per step)             │
│  01: bare ChatClient                         │
│  02: + system prompt + tool + ToolCallAdvisor│
│  03: + full tools + constraint prompt        │
│  04: + AgentLoopAdvisor (turn limits)        │
│  ...                                         │
└──────────────────────────────────────────────┘
```

## Key Design Decisions

1. **Spring AI 2.0.0-M3** — Using the latest milestone. Steps 01-03 use `ToolCallAdvisor.builder().build()` (Spring AI's built-in agent loop — `do { call → execute tool → feed back } while (hasToolCalls)`). Step 04 replaces this with `AgentLoopAdvisor` from agent-workflow which adds turn limits, stuck detection, and cost tracking on top of the same loop.
2. **Simulated data** — Restaurant data is hardcoded in `RestaurantTools.java`. No external database needed. 5 Barcelona restaurants with varying prices, cuisines, dietary options, and noise levels.
3. **Deterministic tools become judges** — `checkExpensePolicy` and `checkDietaryOptions` apply business rules deterministically. In the measurement phase (Mark's portion of the workshop), the same logic becomes the basis for judges that evaluate whether the agent made good decisions.
4. **Google Java Format** — Enforced by Spotless (Adib's existing config).
5. **OpenAI by default** — `application.yml` uses `gpt-4o`. Participants can switch to Anthropic by changing the starter dependency and config.

## Connection to the 8hr Workshop

| Workshop Part | Steps | Lead |
|--------------|-------|------|
| Part 1: Feel the Loop | Hand-cranked (01-hand-cranked-agentic-loop/) | Adib |
| Part 2: Build the Agent | Steps 01-09 | Adib + Christian |
| Part 3: Compose + Wrap | Steps 10-12 (A2A, ACP, wrap) | Christian + Mark |
| Part 4: Measure | agent-journal + agent-judge + agent-bench | Mark |
| Part 5: Improve | markov-analysis + SkillsJars + agent-experiment | Mark |

## Open Questions for Discussion

1. Should Steps 04+ use `agent-workflow` (AgentLoopAdvisor) as a dependency, or implement turn limits directly in the handler? Using agent-workflow shows participants a real library; implementing directly keeps it simple.
2. For Step 06 (MCP), do we run the MCP server as a separate process that participants launch, or embed it in the same app with a different profile?
3. For Steps 10-11 (A2A/ACP), how many separate processes should participants manage? The Airbnb planner example in spring-ai-a2a runs 3 separate apps.
4. Should we provide a Docker Compose for the multi-process steps?
5. API key strategy: shared workshop key, or bring-your-own?
