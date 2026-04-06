---
theme: default
colorSchema: dark
title: "The Art of Building Agents — Spring I/O 2026"
info: |
  ## The Art of Building Agents
  Build · Eval · Analyze · Improve

  Spring I/O Barcelona 2026
class: text-center
drawings:
  persist: false
transition: slide-left
mdc: true
latex: false
---

# The Art of Building Agents

## Build · Eval · Analyze · Improve

**Adib Saikali · Christian Tzolov · Mark Pollack**

Spring I/O Barcelona 2026

<div class="abs-br m-6 flex gap-2">
  <a href="https://github.com/asaikali/art-of-building-agents" target="_blank" class="slidev-icon-btn">
    <carbon:logo-github />
  </a>
</div>

<!--
SPEAKER (≈30s):
"Welcome. Today we're going to build an AI agent from scratch and take it all the way to self-correction. Not a demo — you'll write every line. By the end, you'll have an agent that can evaluate itself and fix its own mistakes."
-->

---
layout: center
class: text-center bg-black
---

<div class="text-5xl font-bold text-white">
Everyone builds agents.
</div>

<v-click>

<div class="text-5xl font-bold text-gradient mt-8">
Nobody measures them.
</div>

</v-click>

<!--
SPEAKER (≈15s):
"Here's the problem. Building an agent is the easy part. Knowing whether it actually works — that's what nobody does."
(CLICK: reveal "Nobody measures them.")
-->

---
layout: default
---

# The Developer Lifecycle

<div class="mt-8 grid grid-cols-5 gap-3 text-center">
  <div class="lifecycle-active">
    <div class="text-2xl font-bold">Build</div>
    <div class="text-sm mt-1 opacity-75">Steps 01-12</div>
  </div>
  <div class="lifecycle-dim">
    <div class="text-2xl font-bold">Test</div>
    <div class="text-sm mt-1 opacity-75">Everyone stops here</div>
  </div>
  <div class="lifecycle-dim">
    <div class="text-2xl font-bold">Eval</div>
    <div class="text-sm mt-1 opacity-75">Step 14</div>
  </div>
  <div class="lifecycle-dim">
    <div class="text-2xl font-bold">Analyze</div>
    <div class="text-sm mt-1 opacity-75">Step 15</div>
  </div>
  <div class="lifecycle-dim">
    <div class="text-2xl font-bold">Improve</div>
    <div class="text-sm mt-1 opacity-75">Step 16</div>
  </div>
</div>

<v-click>

<div class="mt-10 ted-emphasis">
Build and Test are table stakes. Eval is where most teams think they're done. <strong>Analyze</strong> and <strong>Improve</strong> are where the ROI compounds.
</div>

</v-click>

<!--
SPEAKER (≈45s):
"This is the developer lifecycle for agents. Build, Test, Eval, Analyze, Improve. Everyone does Build. Some teams get to Test. Almost nobody goes further. Today we go all the way to Improve — and that's where agents start getting better on their own."
-->

---
layout: default
---

# What We're Building

## Jarvis — Business Dinner Agent for Barcelona

<v-clicks>

* **Domain**: Search restaurants, check expense policy, verify dietary needs, book a table
* **5 Barcelona restaurants** with real constraints (price, vegetarian, noise level)
* **Same domain, 16 steps** — each adds exactly ONE concept
* **Inspector UI** shows chat, state panel, and events in real time

</v-clicks>

<v-click>

<div class="mt-6 p-4 bg-blue-50 dark:bg-blue-900 rounded-lg">
<div class="font-bold text-lg">The Key Insight</div>
<div>The same deterministic tools that power the agent also power the judges. Build once, measure with the same logic.</div>
</div>

</v-click>

<!--
SPEAKER (≈30s):
"Every step uses the same domain. Jarvis plans business dinners in Barcelona. Five restaurants, four tools, real business rules. The beauty is that the same checkExpensePolicy tool the agent calls also becomes the judge that evaluates the agent's output."
-->

---
layout: center
class: text-center
---

# Part 1: Feel the Loop

<div class="text-3xl mt-8 opacity-75">
What happens inside an agent, turn by turn
</div>

<!--
SPEAKER (≈10s):
"Before we build anything with a framework, let's understand what happens inside an agent loop — by doing it manually."
-->

---
layout: default
---

# The Hand-Cranked Agent Loop

## What a Framework Does for You

<div class="mt-6 grid grid-cols-[1fr_auto_1fr_auto_1fr] gap-3 items-center">
  <div class="pipeline-step pipeline-deterministic">User Message</div>
  <div class="pipeline-arrow">→</div>
  <div class="pipeline-step pipeline-llm">LLM Decides</div>
  <div class="pipeline-arrow">→</div>
  <div class="pipeline-step pipeline-deterministic">Execute Tool</div>
</div>

<div class="mt-4 text-center">
  <div class="pipeline-arrow text-3xl">↻</div>
  <div class="text-sm opacity-75">Repeat until no more tool calls</div>
</div>

<v-click>

<div class="mt-6 bullets-small">

* **Turn 1**: User says "Find a restaurant" → LLM calls `searchRestaurants("Eixample")`
* **Turn 2**: Tool returns 3 results → LLM calls `checkExpensePolicy(28, 4)`
* **Turn 3**: Policy OK → LLM calls `checkDietaryOptions("Cervecería Catalana", "vegetarian")`
* **Turn 4**: Dietary OK → LLM responds with recommendation

</div>

</v-click>

<!--
SPEAKER (≈45s):
"This is the agent loop. User message in, LLM decides what to do, tool executes, result goes back to the LLM. Repeat until the LLM decides it has enough information to respond. In Part 1, you do this by hand — you ARE the loop. Call the LLM, read the tool call, execute it yourself, feed the result back."
-->

---
layout: center
class: text-center
---

# Part 2: Build the Agent

<div class="text-3xl mt-8 opacity-75">
Steps 01-09 — From ChatClient to sub-agents
</div>

---
layout: default
---

# Part 2: The Build Progression

<div class="bullets-small bullets-tight">

| Step | What It Adds | Key Concept |
|------|-------------|-------------|
| 01 | Bare ChatClient | Minimum working agent |
| 02 | System prompt + `@Tool` + `ToolCallAdvisor` | First real agent loop |
| 03 | Full tool set + constraint-aware prompt | Guardrails via prompt engineering |
| 04 | `AgentLoopAdvisor` (max turns, stuck detection) | Governed loops |
| 05 | Journal recording to JSONL | The seam between build and measure |
| 06 | MCP server + client | Tools live outside the agent |
| 07 | Memory with LLM compaction | Persistent learning across conversations |
| 08 | Human-in-the-loop | Agent pauses to ask the user |
| 09 | Sub-agent delegation | Coordinator + specialist composition |

</div>

<v-click>

<div class="mt-4 p-3 bg-green-50 dark:bg-green-900 rounded-lg text-sm">
<strong>Dependency boundary</strong>: Steps 01-03 are pure Spring AI. Step 04 introduces the first AgentWorks library.
</div>

</v-click>

<!--
SPEAKER (≈60s):
"Nine steps. Each one adds exactly one concept. Step 1 is a bare ChatClient — you type, the LLM responds. Step 2 adds a tool and a loop. Step 3 adds guardrails via prompt engineering. Step 4 introduces turn limits so the agent can't loop forever. Step 5 starts recording — this is where measurement begins. Then we go multi-process with MCP, add memory, human-in-the-loop, and sub-agents. By Step 9 you have a serious agent."
-->

---
layout: center
class: text-center
---

# Part 3: Compose and Wrap

<div class="text-3xl mt-8 opacity-75">
Steps 10-13 — Protocols, CLI wrapping, structured execution
</div>

---
layout: default
---

# Part 3: Beyond a Single Agent

<div class="bullets-small bullets-tight">

| Step | What It Adds | Key Concept |
|------|-------------|-------------|
| 10 | A2A protocol (expense agent + client) | Agents across processes |
| 11 | ACP endpoint (WebSocket / stdio) | IDE-native agents |
| 12 | Wrap Claude Code CLI | Bring-your-own-agent |
| 13 | Workflow DSL (Stripe pattern) | Deterministic → AI → Validate |

</div>

<v-click>

<div class="mt-8">

### Step 13: The Stripe Pattern

<div class="mt-4 grid grid-cols-[1fr_auto_1fr_auto_1fr] gap-3 items-center">
  <div class="pipeline-step pipeline-deterministic">gather-context<br/><span class="text-xs opacity-75">filter restaurants</span></div>
  <div class="pipeline-arrow">→</div>
  <div class="pipeline-step pipeline-llm">recommend<br/><span class="text-xs opacity-75">LLM picks best</span></div>
  <div class="pipeline-arrow">→</div>
  <div class="pipeline-step pipeline-deterministic">validate<br/><span class="text-xs opacity-75">verify constraints</span></div>
</div>

<div class="mt-3 text-sm text-center opacity-75">
The LLM does ONLY reasoning. Filtering and validation are zero-token, guaranteed correct.
</div>

</div>

</v-click>

<!--
SPEAKER (≈45s):
"Now we compose. Step 10 puts the expense checker in a separate process — agents talking to agents via A2A protocol. Step 11 exposes the agent to your IDE via ACP. Step 12 wraps Claude Code — you don't even need a ChatClient. And Step 13 is the Stripe pattern: sandwich the LLM between deterministic steps. Context, AI, Validate. This is how Stripe processes 1,300 PRs a week."
-->

---
layout: center
class: text-center bg-black
---

<div class="text-4xl text-white">
Build is table stakes.
</div>

<v-click>

<div class="text-5xl font-bold text-gradient mt-8">
What comes after Build is what matters.
</div>

</v-click>

<!--
SPEAKER (≈10s):
"Everything up to here — everyone can do this. What comes next is what separates demo agents from production agents."
(CLICK: reveal the gradient text)
-->

---
layout: center
class: text-center
---

# Part 4: Measure

<div class="text-3xl mt-8 opacity-75">
Steps 14-15 — Eval + Analyze
</div>

---
layout: default
---

# Step 14: Eval with CascadedJury

## "Does my agent work?"

<div class="mt-4">

Three-tier cascade — cheap checks first, LLM only if needed:

</div>

<div class="mt-4 space-y-3">

<v-click>

<div class="grid grid-cols-[80px_1fr_1fr_1fr] gap-3 items-center text-sm">
  <div class="font-bold text-green-400">T0</div>
  <div class="pipeline-step pipeline-deterministic">ExpensePolicyJudge</div>
  <div>Price ≤ €50/person?</div>
  <div class="opacity-75">Free, instant, fail-fast</div>
</div>

</v-click>

<v-click>

<div class="grid grid-cols-[80px_1fr_1fr_1fr] gap-3 items-center text-sm">
  <div class="font-bold text-green-400">T1</div>
  <div class="pipeline-step pipeline-deterministic">DietaryComplianceJudge</div>
  <div>Has vegetarian options?</div>
  <div class="opacity-75">Free, instant, abstains if N/A</div>
</div>

</v-click>

<v-click>

<div class="grid grid-cols-[80px_1fr_1fr_1fr] gap-3 items-center text-sm">
  <div class="font-bold text-purple-400">T2</div>
  <div class="pipeline-step pipeline-llm">RecommendationQualityJudge</div>
  <div>Well-reasoned? Helpful?</div>
  <div class="opacity-75">LLM — only if T0+T1 pass</div>
</div>

</v-click>

</div>

<v-click>

<div class="mt-6 p-3 bg-blue-50 dark:bg-blue-900 rounded-lg text-sm">
If T0 fails (too expensive), the cascade stops. Zero LLM tokens spent on quality assessment.
</div>

</v-click>

<!--
SPEAKER (≈60s):
"Step 14 is eval. Three judges in a cascade. Tier 0: deterministic expense check — same logic the agent uses, now used to evaluate the agent. Free, instant. If the agent recommended a restaurant over 50 euros, we stop right there. Tier 1: dietary compliance — also deterministic. Tier 2: LLM quality judge — only fires if the cheap checks pass. This is the pattern. Deterministic judges are free gates. LLM judges are expensive — only use them when you need reasoning."
-->

---
layout: default
---

# Step 15: Analyze with Trajectory Classification

## "Why did it fail that way?"

<div class="mt-4 text-sm">

Map every tool call to a semantic state:

</div>

<div class="mt-2 grid grid-cols-2 gap-6">

<div>

| Tool Call | State |
|-----------|-------|
| `searchRestaurants` | SEARCH |
| `checkExpensePolicy` | CHECK_BUDGET |
| `checkDietaryOptions` | CHECK_DIETARY |
| `bookTable` | BOOK |

</div>

<div>

<v-click>

**Good trajectory** (efficient):
<div class="mt-1 p-2 bg-green-900 rounded text-sm font-mono">
SEARCH → CHECK_BUDGET → CHECK_DIETARY → BOOK<br/>
States: 4 | Loops: 0 | Efficiency: 100%
</div>

**Bad trajectory** (looping):
<div class="mt-2 p-2 bg-red-900 rounded text-sm font-mono">
SEARCH → SEARCH → SEARCH → CHECK_BUDGET<br/>
States: 4 | Loops: 2 | Efficiency: 50%<br/>
⚠ Hotspot: SEARCH (3 visits)
</div>

</v-click>

</div>

</div>

<v-click>

<div class="mt-4 ted-emphasis text-sm">
Recording + classification = trajectory fingerprint. Now you can see <strong>WHERE</strong> the agent wastes time, not just <strong>WHETHER</strong> it passed.
</div>

</v-click>

<!--
SPEAKER (≈45s):
"Step 15 is the stage nobody else does. We classify every tool call into a semantic state. Then we compute the trajectory: sequence, transitions, loops, hotspots. A good agent goes SEARCH, CHECK_BUDGET, CHECK_DIETARY, BOOK — four states, no loops, 100% efficiency. A bad agent searches three times before checking the budget. Now you know exactly where to fix it."
-->

---
layout: center
class: text-center
---

# Part 5: Improve

<div class="text-3xl mt-8 opacity-75">
Step 16 — Close the Loop
</div>

---
layout: default
---

# Step 16: Self-Correction with Quality Gate

## "What fixes the behavioral gaps?"

<div class="mt-4 grid grid-cols-[1fr_auto_1fr] gap-3 items-center">
  <div class="pipeline-step pipeline-llm">recommend</div>
  <div class="pipeline-arrow">→</div>
  <div class="pipeline-step pipeline-deterministic">JudgeGate<br/><span class="text-xs opacity-75">CascadedJury</span></div>
</div>

<div class="mt-3 grid grid-cols-2 gap-6">

<div class="text-center">
  <div class="pipeline-arrow text-2xl text-green-400">↓ PASS</div>
  <div class="mt-2 pipeline-step pipeline-deterministic">Done</div>
</div>

<div class="text-center">
  <div class="pipeline-arrow text-2xl text-red-400">↓ FAIL</div>
  <div class="mt-2 pipeline-step pipeline-llm">reflect + revise</div>
  <div class="mt-1 pipeline-arrow text-xl">↑ retry (max 2)</div>
</div>

</div>

<v-click>

<div class="mt-6 p-3 bg-yellow-50 dark:bg-yellow-900 rounded-lg text-sm">
<strong>Demo</strong>: "Find a restaurant in Paral·lel" → Tickets Bar (€75) → <span class="text-red-400">FAIL</span> expense policy → feedback → Cervecería Catalana (€28) → <span class="text-green-400">PASS</span>
</div>

</v-click>

<v-click>

```java
Workflow.<String, String>define("jarvis-with-quality-gate")
    .step(recommend)
    .gate(judgeGate).onFail(recommend).withReflector(reflector).maxRetries(2).end()
    .run(userRequest);
```

</v-click>

<!--
SPEAKER (≈60s):
"Step 16 closes the loop. The agent recommends, the judge gate evaluates. If it passes — done. If it fails, the reflector turns the verdict into feedback: 'Price 75 euros exceeds the 50 euro limit. Pick a cheaper option.' The agent revises. Second attempt: Cervecería Catalana at 28 euros. Pass. This is what makes agents get better — the loop from Eval back to Improve, automated."
-->

---
layout: default
---

# The Full Picture

<div class="mt-6 grid grid-cols-5 gap-3 text-center">
  <div class="lifecycle-active">
    <div class="text-xl font-bold">Build</div>
    <div class="text-xs mt-1">Steps 01-13</div>
  </div>
  <div class="lifecycle-active" style="border-color: rgba(109, 179, 63, 0.5);">
    <div class="text-xl font-bold">Test</div>
    <div class="text-xs mt-1">Integration tests</div>
  </div>
  <div class="lifecycle-active">
    <div class="text-xl font-bold">Eval</div>
    <div class="text-xs mt-1">Step 14</div>
  </div>
  <div class="lifecycle-active">
    <div class="text-xl font-bold">Analyze</div>
    <div class="text-xs mt-1">Step 15</div>
  </div>
  <div class="lifecycle-active">
    <div class="text-xl font-bold">Improve</div>
    <div class="text-xs mt-1">Step 16</div>
  </div>
</div>

<v-click>

<div class="mt-8 bullets-small">

| Part | Steps | What You Learn |
|------|-------|---------------|
| Part 1: Feel the Loop | Hand-cranked | What happens inside an agent, turn by turn |
| Part 2: Build | 01-09 | ChatClient → tools → loops → journal → memory → sub-agents |
| Part 3: Compose | 10-13 | A2A, ACP, CLI wrapping, Workflow DSL |
| Part 4: Measure | 14-15 | CascadedJury eval + trajectory analysis |
| Part 5: Improve | 16 | Self-correction via quality gates |

</div>

</v-click>

<v-click>

<div class="mt-4 ted-emphasis text-sm">
16 steps, one domain, one progression. From <code>ChatClient.prompt()</code> to self-correcting agents.
</div>

</v-click>

<!--
SPEAKER (≈30s):
"That's the full lifecycle. Five parts, sixteen steps, one domain. You start with a bare ChatClient and end with an agent that evaluates itself, detects where it wastes time, and self-corrects. Everything in between builds one concept at a time."
-->

---
layout: default
---

# The Spring AI Ecosystem

<div class="mt-4 bullets-small bullets-tight">

| Library | Steps | What It Does |
|---------|-------|-------------|
| **Spring AI** | 01-03 | ChatClient, `@Tool`, ToolCallAdvisor |
| **workflow-core** | 04+ | AgentLoopAdvisor (turn limits, stuck detection) |
| **journal-core** | 05 | Structured recording to JSONL |
| **memory-core** | 07 | Persistent memory with LLM compaction |
| **spring-ai-mcp** | 06 | MCP server + client for tool discovery |
| **spring-ai-a2a** | 10 | Agent-to-agent protocol |
| **acp-java** | 11 | Agent Communication Protocol (IDE integration) |
| **agent-client** | 12 | Wrap CLI agents (Claude Code) as Spring components |
| **workflow-flows** | 13, 16 | Workflow DSL: steps, gates, JudgeGate |
| **agent-judge** | 14-16 | Judge, CascadedJury, DeterministicJudge, LLMJudge |

</div>

<div class="mt-4 text-sm opacity-75 text-center">
All versions managed by <code>agentworks-bom</code> — one version property in the parent POM.
</div>

<!--
SPEAKER (≈30s):
"Here's the ecosystem. Spring AI is the foundation — ChatClient, tools, advisors. On top of that, AgentWorks provides the loop governance, journal, memory, workflow DSL, and judges. All managed by a single BOM. You bump one version property, everything updates."
-->

---
layout: center
class: text-center
---

# Let's Build

<div class="text-5xl font-bold text-gradient mt-8">
cd agents/basic-chatbot
</div>

<div class="text-2xl mt-8 opacity-75">
Step 01 → Step 16
</div>

<div class="abs-br m-6 flex gap-2">
  <a href="https://github.com/asaikali/art-of-building-agents" target="_blank" class="slidev-icon-btn">
    <carbon:logo-github />
  </a>
</div>

<!--
SPEAKER (≈10s):
"Enough slides. Let's build. Open your terminal, cd into agents/basic-chatbot, and let's go."
-->
