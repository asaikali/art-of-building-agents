# Roadmap — Progressive Agent Examples

> **Updated**: 2026-04-02
> **Branch**: `progressive-agent-examples`
> **Stack**: Spring AI 2.0.0-M3 + Spring Boot 4.1.0-M2 + AgentWorks BOM 1.0.4

---

## Developer Lifecycle Framing

```
Build → Test → Eval → Analyze → Improve
                ↑                    ↑
         Everyone stops here    Nobody does this except us
```

Everyone stops at Eval ("did it pass?"). Nobody asks "why did it fail that way?" (Analyze) or "what specific knowledge would fix it?" (Improve). The loop from Eval back to Improve is what makes agents get better over time — and nobody closes it except us.

**Executive framing**: Build and Test are table stakes. Eval is where most teams think they're done. Analyze and Improve are where the ROI compounds — each cycle makes agents measurably faster, cheaper, and more reliable.

---

## Done — Build (Steps 01-12)

- [x] Step 01 `basic-chatbot` — Adib's original: bare ChatClient, no tools
- [x] Step 02 `02-tool-calling` — System prompt + searchRestaurants + ToolCallAdvisor
- [x] Step 03 `03-guardrails` — Full tool set + constraint-aware prompt
- [x] Step 04 `04-turn-limits` — AgentLoopAdvisor (workflow-core, first AgentWorks dep)
- [x] Step 05 `05-journal` — AgentLoopAdvisor + agent-journal recording to JSONL via `JournalLoopListener`
- [x] Step 06 `06-mcp-server` + `06-mcp-client` — Restaurant tools as MCP server, Jarvis discovers via MCP client
- [x] Step 07 `07-memory` — CompactionMemoryAdvisor with FileSystemMemoryStore + gpt-4o-mini compaction
- [x] Step 08 `08-human-in-the-loop` — AskUserQuestionTool with CompletableFuture bridge to Inspector web UI
- [x] Step 09 `09-subagent` — Jarvis delegates to a researcher sub-agent with its own ChatClient, prompt, and tools
- [x] Step 10 `10-a2a-expense` + `10-a2a-client` — Expense policy as A2A agent, Jarvis discovers and calls via A2A protocol
- [x] Step 11 `11-acp` — Jarvis as ACP agent with @AcpAgent/@Prompt. WebSocket + stdio (IDE integration)
- [x] Step 12 `12-wrap-path` — Claude Code wrapped as Spring component via agent-client. No ChatClient, no @Tool — config only.
- [x] All Steps 01-10 pass `run-workshop-tests.sh`. Step 11 passes via ACP in-memory transport test.
- [x] Parent POM with agentworks-bom dependency management
- [x] `docs/progressive-examples-plan.md`, `docs/integration-testing-plan.md`

---

## Done — Steps 13-16: Structured Execution + Eval + Analyze + Improve

### Prerequisite: Bump BOM

- [x] Update root `pom.xml`: `agentworks.version` 1.0.3 → **1.0.4**
  - Gets `workflow-flows:0.3.0`, `workflow-core:0.3.0` (Workflow DSL)
  - `workflow-batch:0.3.0`, `workflow-temporal:0.3.0` (new modules)

---

### Step 13: `13-workflow` — Structured Execution (Stripe Pattern)

**Lifecycle**: Build (structured) — "Can I control what the LLM does vs doesn't?"

**Concept**: Not everything should be a free-form agent loop. The "context → AI → validate" pattern sandwiches the LLM between deterministic steps. Stripe processes 1,300 PRs/week this way.

**Architecture**: Standalone Spring Boot CommandLineRunner (non-Inspector, like Step 12).

**New deps** (managed by agentworks-bom 1.0.4):
- `io.github.markpollack:workflow-flows` — Workflow DSL (`Step`, `Workflow.define()`)
- `org.springframework.ai:spring-ai-starter-model-openai` — ChatClient for AI step

**Design**:

| Step | Type | What It Does |
|------|------|-------------|
| `gather-context` | Deterministic | Parse request → extract constraints → filter restaurants from hardcoded data |
| `recommend` | AI (ChatClient) | LLM picks best option from filtered candidates, explains why |
| `validate` | Deterministic | Verify recommendation is in candidate list, fits budget, meets dietary needs |

```java
Workflow.<DinnerRequest, DinnerResult>define("dinner-planning")
    .step(gatherContext)       // deterministic — zero tokens, instant
    .step(recommend)           // AI — LLM reasoning only
    .step(validate)            // deterministic — guaranteed correct
    .run(request);
```

**Key files**: `DinnerPlanningWorkflow.java`, `WorkflowApplication.java`, `DinnerRequest.java`, `DinnerResult.java`

**Key message**: "The LLM does ONLY what it's good at (reasoning). Deterministic steps handle filtering and validation — zero tokens, guaranteed correct, instant."

---

### Step 14: `14-judge` — Eval with CascadedJury

**Lifecycle**: Eval — "Does my agent work?"

**Concept**: Evaluate Jarvis output with a 3-tier cascaded jury. Deterministic judges run first (fast, free). LLM judges only fire if deterministic tiers pass.

**Architecture**: Inspector-based app (same as Steps 01-09). AgentHandler runs Jarvis, evaluates the response with CascadedJury. Verdict shown in Inspector state panel.

**New deps** (managed by agentworks-bom 1.0.4):
- `org.springaicommunity:agent-judge-core` — Judge, Jury, CascadedJury, Verdict
- `org.springaicommunity:agent-judge-llm` — LLMJudge base class

**Design — 3-Tier Cascade**:

| Tier | Judge | Type | Policy | What It Checks |
|------|-------|------|--------|---------------|
| T0 | `ExpensePolicyJudge` | Deterministic | REJECT_ON_ANY_FAIL | Price ≤ €50/person? Uses same RESTAURANTS data. |
| T1 | `DietaryComplianceJudge` | Deterministic | ACCEPT_ON_ALL_PASS | Restaurant has stated dietary options? |
| T2 | `RecommendationQualityJudge` | LLM | FINAL_TIER | Recommendation well-reasoned and helpful? |

Inspector state panel shows verdict breakdown:
```
| Judge | Status | Reasoning |
|-------|--------|-----------|
| ExpensePolicy | PASS | Price €22 within €50 limit |
| DietaryCompliance | PASS | Teresa Carles has vegetarian |
| RecommendationQuality | PASS | Clear, well-reasoned |
```

**Key files**: `ExpensePolicyJudge.java`, `DietaryComplianceJudge.java`, `RecommendationQualityJudge.java`, `JudgeHandler.java`, `JudgeApplication.java`

**Key message**: "The same deterministic tools that power the agent (checkExpensePolicy) now power the judges. Build once, measure with the same logic."

---

### Step 15: `15-trajectory` — Analyze with Trajectory Classification

**Lifecycle**: Analyze — "Why did it fail that way?"

**Concept**: Classify tool calls into semantic states, detect loops and hotspots. This is the stage nobody else does.

**Architecture**: Inspector-based app. Builds on Step 05 (journal) + Step 14 (judges). After each conversation, shows trajectory analysis in Inspector state panel.

**New deps**: Same as Step 14 + `journal-core`

**Design — Semantic State Classifier**:

| Tool Call | Semantic State |
|-----------|---------------|
| `searchRestaurants` | SEARCH |
| `checkExpensePolicy` | CHECK_BUDGET |
| `checkDietaryOptions` | CHECK_DIETARY |
| `bookTable` | BOOK |
| (no tool — agent talking) | CLARIFY |

`TrajectoryClassifier` computes:
- State sequence: `[SEARCH, CHECK_BUDGET, CHECK_DIETARY, BOOK]`
- Transition counts: `SEARCH→CHECK_BUDGET: 2, ...`
- Loop detection: same tool called N times in sequence
- Hotspot: most-visited state + self-loop count
- Efficiency score: `productive_states / total_states`

Inspector state panel shows:
```
## Trajectory Analysis

Sequence: SEARCH → CHECK_BUDGET → CHECK_DIETARY → BOOK
States: 4 | Loops: 0 | Efficiency: 100%

⚠ Hotspot: SEARCH (3 visits, 2 self-loops)
→ Agent searched 3 times before checking budget — consider reordering
```

**Key files**: `TrajectoryClassifier.java`, `TrajectoryHandler.java`, `TrajectoryApplication.java`

**Key message**: "Recording + classification = trajectory fingerprint. Now you can see WHERE the agent wastes time, not just WHETHER it passed."

---

### Step 16: `16-quality-gate` — Improve with Self-Correction Loop

**Lifecycle**: Improve — "What knowledge fixes the behavioral gaps?"

**Concept**: Close the loop. Agent generates → judge evaluates → on failure, feedback drives revision. This is the stage that makes agents get better over time.

**Architecture**: Standalone Spring Boot CommandLineRunner. Uses Workflow DSL gate pattern.

**New deps**:
- `io.github.markpollack:workflow-flows` — Workflow DSL + Gate
- `org.springaicommunity:agent-judge-core` — JudgeGate
- `org.springaicommunity:agent-judge-llm` — LLM judge
- `org.springframework.ai:spring-ai-starter-model-openai`

**Design**:

```java
Workflow.<String, String>define("jarvis-with-quality-gate")
    .step(recommend)              // AI: Jarvis generates recommendation
    .gate(judgeGate)              // CascadedJury (threshold 0.8)
        .onPass(formatResult)     // All checks passed → present to user
        .onFail(revise)           // Failed → revise with feedback
        .withReflector(reflector) // Verdict → constructive guidance
        .maxRetries(2)            // Cap at 2 revision attempts
    .end()
    .run(userRequest);
```

**Demo scenario**: Default goal deliberately triggers failure — "Find a restaurant in Paral·lel for a team lunch" picks Tickets Bar (€75) which fails expense policy (€50 limit). Agent self-corrects:

```
Attempt 1: Tickets Bar (€75/person) → FAIL: expense policy
Feedback: "Price €75 exceeds €50 limit. Recommend a cheaper option."
Attempt 2: Cervecería Catalana (€28/person) → PASS
```

**Key files**: `QualityGateWorkflow.java`, `QualityGateApplication.java`

**Key message**: "The loop from Eval back to Improve is what makes agents get better. The gate pattern automates it — generate, judge, reflect, revise."

---

## Build Order

1. Bump BOM (root `pom.xml` 1.0.3 → 1.0.4)
2. Step 13 (`13-workflow`) — independent, no Inspector
3. Step 14 (`14-judge`) — independent, Inspector-based
4. Step 15 (`15-trajectory`) — builds on Step 14 judge classes + Step 05 journal
5. Step 16 (`16-quality-gate`) — independent, no Inspector

## Lifecycle Mapping

```
Step 01-12: Build ──────────────────────── "Can I create an agent?"
Step 13:    Build (structured) ─────────── "Can I control what the LLM does vs doesn't?"
Step 14:    Eval ───────────────────────── "Does my agent work?"
Step 15:    Analyze ────────────────────── "Why did it fail that way?"
Step 16:    Improve ────────────────────── "What fixes the behavioral gaps?"
                                            ↑
                                     This is where ROI compounds
```

---

## Backlog

- [ ] Pre-built PetClinic agent for 2hr workshop (Mark's session)
- [ ] Slide decks coordination with Adib + Christian
- [ ] Infrastructure: Codespaces/Gitpod, API key strategy
- [ ] Update research project roadmap at `~/tuvium/projects/tuvium-workshop-research/`
