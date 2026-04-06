# Workshop Modules — The Art of Building Agents

> 16 progressive steps from bare ChatClient to self-correcting agents.
> Each module is a standalone Spring Boot app using the same Jarvis restaurant domain.

## Developer Lifecycle

```
Step 01-12: Build ──────────────────────── "Can I create an agent?"
Step 13:    Build (structured) ─────────── "Can I control what the LLM does vs doesn't?"
Step 14:    Eval ───────────────────────── "Does my agent work?"
Step 15:    Analyze ────────────────────── "Why did it fail that way?"
Step 16:    Improve ────────────────────── "What fixes the behavioral gaps?"
```

## All Steps

| Step | Module | What It Adds | Architecture | New Dependency |
|------|--------|-------------|--------------|----------------|
| 01 | `basic-chatbot` | Bare ChatClient, no tools — pass-through to the model | Inspector | Spring AI |
| 02 | `02-tool-calling` | System prompt + `searchRestaurants` + `ToolCallAdvisor`. First real agent. | Inspector | Spring AI |
| 03 | `03-guardrails` | Full tool set (search, expense, dietary, book) + constraint-aware prompt | Inspector | Spring AI |
| 04 | `04-turn-limits` | `AgentLoopAdvisor` — max turns, stuck detection, grace turn recovery | Inspector | **workflow-core** |
| 05 | `05-journal` | Agent-journal recording via `JournalLoopListener`. Every turn to JSONL. | Inspector | **journal-core** |
| 06a | `06-mcp-server` | Restaurant tools as MCP server (port 8081, Streamable HTTP) | Headless | spring-ai-mcp-server |
| 06b | `06-mcp-client` | Jarvis discovers tools dynamically via `SyncMcpToolCallbackProvider` | Inspector | spring-ai-mcp-client |
| 07 | `07-memory` | `CompactionMemoryAdvisor` + `FileSystemMemoryStore`. LLM-powered compaction. | Inspector | **memory-core** |
| 08 | `08-human-in-the-loop` | `AskUserQuestionTool` — agent pauses mid-loop, `CompletableFuture` bridge to UI | Inspector | **spring-ai-agent-utils** |
| 09 | `09-subagent` | Jarvis delegates to researcher sub-agent with isolated ChatClient + tools | Inspector | Spring AI |
| 10a | `10-a2a-expense` | Expense policy checker as Google A2A agent (port 8082) | A2A server | spring-ai-a2a-server |
| 10b | `10-a2a-client` | Jarvis discovers + calls remote A2A expense agent via AgentCard | Inspector | a2a-java-sdk-client |
| 11 | `11-acp` | Jarvis as ACP endpoint — `@AcpAgent` + `@Prompt`. WebSocket or stdio (IDE). | ACP transport | **acp-java** |
| 12 | `12-wrap-path` | Wrap Claude Code CLI as Spring component via `AgentClient`. Config only. | CLI Runner | **agent-client** |
| 13 | `13-workflow` | Workflow DSL: deterministic → AI → validate sandwich (Stripe pattern) | CLI Runner | **workflow-flows** |
| 14 | `14-judge` | CascadedJury: 3-tier eval (deterministic T0/T1, LLM T2). Verdict in state panel. | Inspector | **agent-judge-core/llm** |
| 15 | `15-trajectory` | Tool call classification → semantic states. Loop/hotspot detection + efficiency. | Inspector | agent-judge (same) |
| 16 | `16-quality-gate` | Workflow gate + JudgeGate: agent self-corrects on failure. Reflect → revise. | CLI Runner | **workflow-flows + agent-judge** |

## Dependency Boundaries

**Steps 01-03** — Pure Spring AI. No external dependencies.

**Step 04** — First AgentWorks library enters (`workflow-core` for `AgentLoopAdvisor`).

**Steps 05-09** — Progressive AgentWorks: journal, memory, human-in-the-loop, sub-agents.

**Steps 10-12** — Protocol and composition: A2A, ACP, Claude Code wrapping.

**Steps 13-16** — Structured execution + eval/analyze/improve: Workflow DSL, CascadedJury, trajectory, quality gates.

All library versions managed by `agentworks-bom` in the root POM.

## Step Details

### Step 01: Basic Chatbot

Adib's original. Bare `ChatClient` with no system prompt, no tools. User message goes in, LLM response comes out. Establishes the Inspector scaffold that all subsequent steps build on.

**Key class**: `BasicChatbotHandler` — implements `AgentHandler.onMessage()`, calls `chatClient.prompt().messages(history).call().content()`.

---

### Step 02: Tool Calling

First real agent. Adds a system prompt ("You are Jarvis...") and `searchRestaurants` as a `@Tool`. `ToolCallAdvisor` handles the agent loop: call LLM → if tool call, execute → feed result back → repeat.

**Key class**: `ToolCallingHandler` — wires `ToolCallAdvisor.builder().build()` as advisor.

---

### Step 03: Guardrails

Full tool set: `searchRestaurants`, `checkExpensePolicy`, `checkDietaryOptions`, `bookTable`. System prompt now includes explicit tool selection rules: "Use `checkExpensePolicy` BEFORE recommending any restaurant."

**Key class**: `GuardrailsHandler` — multi-constraint prompt. Same `ToolCallAdvisor`.

---

### Step 04: Turn Limits

Replaces Spring AI's `ToolCallAdvisor` with `AgentLoopAdvisor` from AgentWorks. Adds hard stop after N turns, stuck-loop detection, and grace turn recovery. First external library.

**Key class**: `TurnLimitsHandler` — `AgentLoopAdvisor.builder().maxTurns(15).build()`.

---

### Step 05: Journal Recording

Wires `agent-journal` via `JournalLoopListener` on `AgentLoopAdvisor`. Every interaction recorded to JSONL: turns, tokens, cost, termination reason. This is the seam between "build" and "measure."

**Key classes**: `JournalHandler` (creates journal `Run` per interaction), `JournalLoopListener` (bridges advisor events to journal events).

---

### Step 06: MCP (Server + Client)

**06-mcp-server**: Restaurant tools extracted into standalone MCP server on port 8081 (Streamable HTTP transport).

**06-mcp-client**: Jarvis agent discovers tools dynamically via `SyncMcpToolCallbackProvider`. No compiled-in tool references.

**Key concept**: Tool decoupling — agent doesn't know what tools exist until runtime.

---

### Step 07: Memory

Adds `CompactionMemoryAdvisor` with `FileSystemMemoryStore`. Agent remembers context across conversations. When memory exceeds token budget, an LLM (gpt-4o-mini) compacts it into patterns.

**Key class**: `MemoryHandler` — chains `CompactionMemoryAdvisor` + `AgentLoopAdvisor`.

---

### Step 08: Human-in-the-Loop

`AskUserQuestionTool` lets the agent pause mid-loop to ask for clarification. `CompletableFuture` bridge connects the tool call to the Inspector web UI. Re-entrant handler detects pending questions.

**Key class**: `HumanInTheLoopHandler` — injects `InspectorQuestionHandler` that completes futures.

---

### Step 09: Sub-Agent Delegation

Jarvis becomes a coordinator. Instead of calling tools directly, it uses `delegateToResearcher` — a tool that spawns a fresh `ChatClient` with its own system prompt and restaurant tools. Isolated context per research request.

**Key classes**: `SubagentHandler` (coordinator), `ResearcherTool` (spawns sub-agent with fresh ChatClient per call).

---

### Step 10: A2A Protocol (Server + Client)

**10-a2a-expense**: Expense policy checker exposed as A2A agent with `AgentCard` discovery + JSON-RPC messages.

**10-a2a-client**: Jarvis discovers the remote agent at startup, calls it for expense checks while keeping search/dietary/book local.

**Key concept**: Protocol-based composition — agents across processes via standardized interface.

---

### Step 11: ACP Agent

Jarvis exposed as ACP endpoint with `@AcpAgent`, `@Prompt`, `@Initialize`. Works over WebSocket (demos) or stdio (IDE integration — IntelliJ, Zed, VS Code).

**Key concept**: Same agent logic, different transport — IDE-native.

---

### Step 12: Wrap Path

Wraps Claude Code CLI as a Spring component via `AgentClient.Builder`. No `ChatClient`, no `@Tool` — all configuration. The agent loop runs inside Claude Code, not in Java.

**Key class**: `WrapPathApplication` — `CommandLineRunner` that calls `agentClient.run(goal)`.

---

### Step 13: Workflow DSL (Stripe Pattern)

Three-step sandwich: **gather-context** (deterministic, filters restaurants) → **recommend** (AI, LLM picks best option) → **validate** (deterministic, checks recommendation against constraints).

The LLM does ONLY reasoning. Filtering and validation are zero-token, guaranteed correct.

**Key class**: `DinnerPlanningWorkflow` — `Workflow.<DinnerRequest, DinnerResult>define("dinner-planning").step(gatherContext).step(recommend).step(validate).run(request)`.

---

### Step 14: Judge (CascadedJury)

3-tier cascaded evaluation. T0: `ExpensePolicyJudge` (deterministic, fails fast if price > EUR 50). T1: `DietaryComplianceJudge` (deterministic, abstains if no dietary need). T2: `RecommendationQualityJudge` (LLM, only fires if T0-T1 pass).

Verdict breakdown shown in Inspector state panel.

**Key class**: `JudgeHandler` — runs Jarvis agent loop, then evaluates output with `CascadedJury.vote()`.

---

### Step 15: Trajectory Analysis

Classifies tool calls into semantic states: SEARCH, CHECK_BUDGET, CHECK_DIETARY, BOOK, CLARIFY. Computes state sequence, transition counts, loop detection, hotspot analysis, and efficiency score.

Combined trajectory + verdict analysis in the Inspector state panel.

**Key classes**: `TrajectoryClassifier` (pure Java classifier), `ToolCallTracker` (thread-local recording), `TrajectoryHandler`.

---

### Step 16: Quality Gate (Self-Correction)

Closes the loop. Workflow DSL gate pattern: recommend → `JudgeGate` evaluates → on failure, reflector transforms verdict into feedback → agent revises. Max 2 retries.

Default demo targets Paral-lel where Tickets Bar (EUR 75) fails expense policy. Agent self-corrects to a cheaper option.

**Key class**: `QualityGateWorkflow` — `Workflow.define().step(recommend).gate(judgeGate).onFail(recommend).withReflector(reflector).maxRetries(2).end().run()`.

---

## Workshop Parts Mapping

| Workshop Part | Steps | Lead | Focus |
|--------------|-------|------|-------|
| Part 1: Feel the Loop | Hand-cranked agent loop | Adib | Manual tool-call dispatch |
| Part 2: Build the Agent | Steps 01-09 | Adib + Christian | Progressive agent construction |
| Part 3: Compose + Wrap | Steps 10-13 | Christian + Mark | A2A, ACP, CLI wrapping, Workflow DSL |
| Part 4: Measure | Steps 14-15 | Mark | CascadedJury eval + trajectory analysis |
| Part 5: Improve | Step 16 | Mark | Self-correction via quality gates |
