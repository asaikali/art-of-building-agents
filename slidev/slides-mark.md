---
theme: default
colorSchema: dark
title: "The Art of Building Agents — Mark's Modules"
info: |
  ## The Art of Building Agents
  Journal · Workflow · Judge · Trajectory

  Spring I/O Barcelona 2026
class: text-center
drawings:
  persist: false
transition: slide-left
mdc: true
latex: false
---

# The Art of Building Agents

## Journal · Workflow · Judge · Trajectory

**Mark Pollack**

Spring I/O Barcelona 2026

<!--
SPEAKER (≈20s):
"My four modules take us from recording what the agent does, to structuring its execution, to evaluating it, to understanding exactly where it fails. Build is table stakes — this is what comes after."
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
"Everyone can build an agent. The question is: can you measure it, understand it, and make it better?"
(CLICK: reveal the gradient text)
-->

---
layout: default
---

# Four Modules, One Progression

<div class="mt-8 grid grid-cols-4 gap-4 text-center">
  <div class="p-4 rounded-lg border-2 border-blue-500 bg-blue-900/30">
    <div class="text-2xl font-bold">05</div>
    <div class="text-lg mt-1">Journal</div>
    <div class="text-xs mt-2 opacity-75">Record everything</div>
  </div>
  <div class="p-4 rounded-lg border-2 border-green-500 bg-green-900/30">
    <div class="text-2xl font-bold">13</div>
    <div class="text-lg mt-1">Workflow</div>
    <div class="text-xs mt-2 opacity-75">Structure execution</div>
  </div>
  <div class="p-4 rounded-lg border-2 border-purple-500 bg-purple-900/30">
    <div class="text-2xl font-bold">14</div>
    <div class="text-lg mt-1">Judge</div>
    <div class="text-xs mt-2 opacity-75">Evaluate output</div>
  </div>
  <div class="p-4 rounded-lg border-2 border-orange-500 bg-orange-900/30">
    <div class="text-2xl font-bold">15</div>
    <div class="text-lg mt-1">Trajectory</div>
    <div class="text-xs mt-2 opacity-75">Understand behavior</div>
  </div>
</div>

<v-click>

<div class="mt-8 text-center opacity-75">

**Record** → **Structure** → **Evaluate** → **Understand**

Without recording, you can't measure. Without structure, you can't control. Without evaluation, you can't improve. Without trajectory, you can't explain.

</div>

</v-click>

<!--
SPEAKER (≈30s):
"Four modules. Journal records what the agent does. Workflow structures how it executes. Judge evaluates whether the output is correct. Trajectory tells you WHY it failed — not just that it failed."
-->

---
layout: center
class: text-center
---

# Step 05: Journal

<div class="text-3xl mt-8 opacity-75">
The seam between Build and Measure
</div>

<!--
SPEAKER (≈10s):
"Step 05 is where we start recording. Every turn, every tool call, every token — to JSONL. This is the seam between building and measuring."
-->

---
layout: default
---

# Journal — Why Record?

<div class="mt-6 space-y-4">

<v-clicks>

* **Without recording, you can't measure.** Without measurement, you can't improve.
* Journal data feeds everything downstream: trajectory analysis, cost tracking, eval scoring
* Append-only JSONL — one file per run, structured events per turn
* The same data powers heatmaps, tool-call sequences, and cost analysis

</v-clicks>

</div>

<v-click>

<div class="mt-6 p-4 bg-blue-50 dark:bg-blue-900 rounded-lg">
<div class="font-bold text-lg">Key Insight</div>
<div>Recording is not logging. Logging is for debugging. Journal is for measurement — structured, queryable, append-only.</div>
</div>

</v-click>

<!--
SPEAKER (≈30s):
"This isn't logging. Logging is 'print something and hope someone reads it.' Journal is structured measurement. Every turn creates a typed event with turn number, tokens, cost, termination reason. It's what makes everything in Steps 14 and 15 possible."
-->

---
layout: default
---

# Journal — How It Works

<div class="mt-4">

### 1. Configure once at startup

```java
Journal.configure(new JsonFileStorage(Path.of(".agent-journal")));
```

### 2. Create a Run per interaction

```java
try (Run run = Journal.run("jarvis-dinner-planning")
        .config("model", "gpt-4o")
        .config("maxTurns", 15)
        .tag("step", "05-journal")
        .start()) {
    // agent loop executes here
}
```

### 3. Wire the listener into the advisor

```java
var advisor = AgentLoopAdvisor.builder()
    .toolCallingManager(ToolCallingManager.builder().build())
    .maxTurns(15)
    .listener(new JournalLoopListener(run))
    .build();
```

</div>

<!--
SPEAKER (≈45s):
"Three steps. Configure the journal with a storage backend — here it's JSONL files. Create a Run for each user interaction, tagging it with metadata. Then wire a JournalLoopListener into the AgentLoopAdvisor. The listener bridges advisor lifecycle events into structured journal events. That's it — the agent records itself."
-->

---
layout: default
---

# Journal — What Gets Recorded

<div class="mt-4 grid grid-cols-2 gap-6">

<div>

### Lifecycle Events

| Event | Data |
|-------|------|
| `loop_started` | runId, userMessage |
| `turn_started` | runId, turn number |
| `turn_completed` | runId, turn, terminationReason |
| `loop_completed` | turns, tokens, cost, reason |

</div>

<div>

### Completion Metrics

```java
run.logEvent(CustomEvent.of(
    "loop_completed",
    Map.of(
      "turnsCompleted", state.currentTurn(),
      "totalTokens", state.totalTokensUsed(),
      "estimatedCost", state.estimatedCost(),
      "reason", reason.name()
    )));
```

</div>

</div>

<v-click>

<div class="mt-4 p-3 bg-green-50 dark:bg-green-900 rounded-lg text-sm">
<strong>Output</strong>: <code>.agent-journal/experiments/jarvis-dinner-planning/runs/&lt;run-id&gt;/</code> — one JSONL file per run, append-only, queryable.
</div>

</v-click>

<!--
SPEAKER (≈30s):
"Four event types. Loop started, turn started, turn completed, loop completed. The completion event carries the metrics you care about: how many turns, how many tokens, estimated cost, and why the loop stopped. All structured, all queryable."
-->

---
layout: default
---

# Journal — Architecture

<div class="mt-8 text-center font-mono text-sm">

```
User Input
  ↓
JournalHandler.onMessage()
  ├─ Journal.run().start()          ← creates Run
  ├─ AgentLoopAdvisor
  │   └─ JournalLoopListener        ← bridges events
  │       ├─ onLoopStarted()
  │       ├─ onTurnStarted()
  │       ├─ onTurnCompleted()
  │       └─ onLoopCompleted()       ← tokens, cost, reason
  ↓
.agent-journal/  (JSONL)
```

</div>

<v-click>

<div class="mt-6 p-4 bg-yellow-50 dark:bg-yellow-900 rounded-lg text-sm">
<strong>Dependencies</strong>: <code>journal-core</code> (recording) + <code>workflow-core</code> (AgentLoopAdvisor). Both managed by <code>agentworks-bom</code>.
</div>

</v-click>

<!--
SPEAKER (≈15s):
"The architecture is simple. Handler creates a Run, wires the listener into the advisor, and the listener bridges lifecycle events to JSONL. The journal data is what we consume in Steps 14 and 15."
-->

---
layout: center
class: text-center
---

# Step 13: Workflow DSL

<div class="text-3xl mt-8 opacity-75">
The Stripe Pattern — Deterministic → AI → Validate
</div>

<!--
SPEAKER (≈10s):
"Step 13 changes how the agent executes. Instead of a free-form tool-calling loop, we sandwich the LLM between deterministic steps."
-->

---
layout: default
---

# Workflow — The Problem with Free-Form Loops

<div class="mt-6 space-y-4">

<v-clicks>

* In Steps 01-12, the LLM decides **everything**: which tools to call, in what order, when to stop
* The LLM might search 5 times before checking the budget
* The LLM might recommend a restaurant it never validated
* The LLM might hallucinate a restaurant that doesn't exist

</v-clicks>

</div>

<v-click>

<div class="mt-6 p-4 bg-red-50 dark:bg-red-900 rounded-lg">
<div class="font-bold text-lg">The Insight</div>
<div>The LLM is good at reasoning. It's bad at filtering and validation. Don't let it do what deterministic code does better.</div>
</div>

</v-click>

<!--
SPEAKER (≈30s):
"In a free-form loop, the LLM controls everything. It might search three times, skip budget checks, or recommend a restaurant it never validated. The workflow pattern fixes this: deterministic code does filtering and validation. The LLM only does what it's good at — reasoning over a curated set."
-->

---
layout: default
---

# Workflow — The Stripe Pattern

<div class="mt-6 grid grid-cols-[1fr_auto_1fr_auto_1fr] gap-3 items-center">
  <div class="p-4 rounded-lg border-2 border-green-500 bg-green-900/30 text-center">
    <div class="font-bold">gather-context</div>
    <div class="text-xs mt-1 opacity-75">Filter restaurants</div>
    <div class="text-xs mt-1 text-green-400">0 tokens</div>
  </div>
  <div class="text-3xl opacity-50">→</div>
  <div class="p-4 rounded-lg border-2 border-purple-500 bg-purple-900/30 text-center">
    <div class="font-bold">recommend</div>
    <div class="text-xs mt-1 opacity-75">LLM picks best</div>
    <div class="text-xs mt-1 text-purple-400">~200 tokens</div>
  </div>
  <div class="text-3xl opacity-50">→</div>
  <div class="p-4 rounded-lg border-2 border-green-500 bg-green-900/30 text-center">
    <div class="font-bold">validate</div>
    <div class="text-xs mt-1 opacity-75">Verify constraints</div>
    <div class="text-xs mt-1 text-green-400">0 tokens</div>
  </div>
</div>

<v-click>

<div class="mt-8 text-center text-sm opacity-75">
The LLM does ONLY reasoning. Filtering and validation are zero-token, guaranteed correct.
</div>

</v-click>

<v-click>

<div class="mt-4 p-3 bg-blue-50 dark:bg-blue-900 rounded-lg text-sm">
This is how Stripe processes <strong>1,300 PRs a week</strong>. Deterministic steps handle structure. AI handles judgment.
</div>

</v-click>

<!--
SPEAKER (≈30s):
"Three steps in a sandwich. Gather-context filters restaurants deterministically — neighborhood, budget, dietary. Zero tokens. The LLM only sees candidates that already match. Then validate checks that the LLM's pick actually exists in the candidate list and is within budget. Zero tokens, guaranteed correct."
-->

---
layout: default
---

# Workflow — Step 1: gather-context

<div class="mt-4 text-sm">

Deterministic filtering — budget, neighborhood, cuisine, dietary:

</div>

```java
Step<DinnerRequest, GatherResult> gatherContext =
    Step.named("gather-context", (ctx, req) -> {
        List<Map<String, Object>> candidates =
            RESTAURANTS.stream()
                .filter(r -> {
                    boolean matchNeighborhood =
                        req.neighborhood() == null ||
                        r.get("neighborhood").toString()
                         .equalsIgnoreCase(req.neighborhood());
                    boolean withinBudget =
                        (int) r.get("pricePerPerson") <= req.budgetPerPerson();
                    boolean meetsDietary =
                        req.dietary() == null ||
                        (boolean) r.get("vegetarianOptions");
                    return matchNeighborhood && withinBudget && meetsDietary;
                })
                .toList();
        return new GatherResult(candidates, req);
    });
```

<v-click>

<div class="mt-3 text-sm text-green-400 text-center">
Zero tokens. Instant. Guaranteed correct. The LLM never sees invalid candidates.
</div>

</v-click>

<!--
SPEAKER (≈20s):
"Pure Java stream filtering. Neighborhood, budget, dietary — all deterministic. The LLM never sees restaurants that don't match. This is the foundation of the sandwich."
-->

---
layout: default
---

# Workflow — Step 2: recommend

<div class="mt-4 text-sm">

LLM receives only pre-filtered candidates:

</div>

```java
Step<GatherResult, RecommendResult> recommend =
    Step.named("recommend", (ctx, gathered) -> {
        if (gathered.candidates().isEmpty()) {
            return new RecommendResult(gathered,
                "No restaurants match all constraints.");
        }

        String candidateList = gathered.candidates().stream()
            .map(r -> String.format("- %s (%s, €%d/person, vegetarian: %s)",
                r.get("name"), r.get("neighborhood"),
                r.get("pricePerPerson"), r.get("vegetarianOptions")))
            .collect(Collectors.joining("\n"));

        String prompt = String.format("""
            Pick the BEST restaurant for a business dinner.
            Party: %d | Budget: €%.0f/person | Dietary: %s

            Candidates:
            %s

            Name the restaurant and explain why (2-3 sentences).""",
            gathered.request().partySize(),
            gathered.request().budgetPerPerson(),
            gathered.request().dietary(), candidateList);

        String reply = chatClient.prompt().user(prompt).call().content();
        return new RecommendResult(gathered, reply);
    });
```

<!--
SPEAKER (≈20s):
"The LLM gets a focused prompt with only valid candidates. Small context, clear task, minimal tokens. It does what it's good at — reasoning and selection — not filtering."
-->

---
layout: default
---

# Workflow — Step 3: validate

<div class="mt-4 text-sm">

Deterministic verification — catches hallucinations and constraint violations:

</div>

```java
Step<RecommendResult, DinnerResult> validate =
    Step.named("validate", (ctx, rec) -> {
        // Does the recommendation reference an actual candidate?
        Map<String, Object> matched = rec.gathered().candidates().stream()
            .filter(r -> rec.recommendation().toLowerCase()
                .contains(r.get("name").toString().toLowerCase()))
            .findFirst().orElse(null);

        if (matched == null) {
            return new DinnerResult(candidates, rec.recommendation(),
                false, "Recommended restaurant not in candidate list");
        }

        int price = (int) matched.get("pricePerPerson");
        if (price > rec.gathered().request().budgetPerPerson()) {
            return new DinnerResult(candidates, rec.recommendation(),
                false, String.format("%s costs €%d, exceeds budget",
                    matched.get("name"), price));
        }

        return new DinnerResult(candidates, rec.recommendation(),
            true, String.format("Validated: %s at €%d/person",
                matched.get("name"), price));
    });
```

<!--
SPEAKER (≈20s):
"Validate catches two things: hallucinated restaurants that aren't in the candidate list, and constraint violations. Zero tokens, guaranteed correct. If the LLM hallucinates, the workflow catches it."
-->

---
layout: default
---

# Workflow — Wiring It Together

<div class="mt-6">

```java
record DinnerRequest(
    String neighborhood, String cuisine, int partySize,
    double budgetPerPerson, String dietary) {}

record DinnerResult(
    List<Map<String, Object>> candidates, String recommendation,
    boolean valid, String validationMessage) {}
```

</div>

<v-click>

<div class="mt-4">

### The entire workflow — three lines:

```java
return Workflow.<DinnerRequest, DinnerResult>define("dinner-planning")
    .step(gatherContext)
    .step(recommend)
    .step(validate)
    .run(request);
```

</div>

</v-click>

<v-click>

<div class="mt-6 p-4 bg-blue-50 dark:bg-blue-900 rounded-lg">
<div class="font-bold text-lg">Type Safety</div>
<div>Each step has explicit input/output types. <code>GatherResult</code> → <code>RecommendResult</code> → <code>DinnerResult</code>. The compiler catches mismatches.</div>
</div>

</v-click>

<!--
SPEAKER (≈20s):
"The workflow DSL is three lines. Define, step, step, step, run. Type-safe: the compiler catches mismatches between steps. The data model is records — immutable, transparent."
-->

---
layout: center
class: text-center
---

# Step 14: Judge

<div class="text-3xl mt-8 opacity-75">
Does my agent work?
</div>

<!--
SPEAKER (≈10s):
"Step 14 answers the most basic question: does this agent produce correct output? Not 'does it run without errors' — does it give the RIGHT answer?"
-->

---
layout: default
---

# Judge — The Eval Problem

<div class="mt-6 space-y-4">

<v-clicks>

* Unit tests check: "Does the code compile and run?"
* Integration tests check: "Do the pieces connect?"
* **Eval checks: "Is the output correct for this domain?"**
* An agent can run flawlessly and still recommend a €75 restaurant on a €50 budget

</v-clicks>

</div>

<v-click>

<div class="mt-6 p-4 bg-red-50 dark:bg-red-900 rounded-lg">
<div class="font-bold text-lg">The Gap</div>
<div>LLM outputs are non-deterministic. You can't assert on exact strings. You need <strong>judges</strong> — evaluators that understand domain constraints.</div>
</div>

</v-click>

<!--
SPEAKER (≈30s):
"Tests tell you if the code works. Eval tells you if the OUTPUT is correct. An agent can pass every test and still recommend a restaurant that's too expensive. You need judges — evaluators that understand your business rules."
-->

---
layout: default
---

# Judge — CascadedJury: Cheap Checks First

<div class="mt-4">

Three-tier cascade — deterministic gates before LLM:

</div>

<div class="mt-4 space-y-3">

<v-click>

<div class="grid grid-cols-[80px_1fr_1fr_1fr] gap-3 items-center text-sm">
  <div class="font-bold text-green-400">T0</div>
  <div class="p-2 rounded border border-green-500 bg-green-900/30">ExpensePolicyJudge</div>
  <div>Price ≤ €50/person?</div>
  <div class="opacity-75">Free, instant, fail-fast</div>
</div>

</v-click>

<v-click>

<div class="grid grid-cols-[80px_1fr_1fr_1fr] gap-3 items-center text-sm">
  <div class="font-bold text-green-400">T1</div>
  <div class="p-2 rounded border border-green-500 bg-green-900/30">DietaryComplianceJudge</div>
  <div>Has vegetarian options?</div>
  <div class="opacity-75">Free, instant, abstains if N/A</div>
</div>

</v-click>

<v-click>

<div class="grid grid-cols-[80px_1fr_1fr_1fr] gap-3 items-center text-sm">
  <div class="font-bold text-purple-400">T2</div>
  <div class="p-2 rounded border border-purple-500 bg-purple-900/30">RecommendationQualityJudge</div>
  <div>Well-reasoned? Helpful?</div>
  <div class="opacity-75">LLM — only if T0+T1 pass</div>
</div>

</v-click>

</div>

<v-click>

<div class="mt-6 p-3 bg-blue-50 dark:bg-blue-900 rounded-lg text-sm">
If T0 fails (too expensive), the cascade <strong>stops</strong>. Zero LLM tokens spent on quality assessment.
</div>

</v-click>

<!--
SPEAKER (≈45s):
"Three judges in a cascade. Tier 0: deterministic expense check — same logic the agent uses, now used to EVALUATE the agent. Free, instant. If the agent recommended a restaurant over 50 euros, we stop right there — no point checking quality. Tier 1: dietary compliance, also deterministic. Tier 2: LLM quality judge, only fires if the cheap checks pass."
-->

---
layout: default
---

# Judge — Deterministic Judge (T0)

<div class="mt-4 text-sm">

Same RESTAURANTS data powers both the agent's tools and the judge:

</div>

```java
public class ExpensePolicyJudge extends DeterministicJudge {
    private static final double EXPENSE_LIMIT = 50.0;

    @Override
    public Judgment judge(JudgmentContext context) {
        String output = context.agentOutput().orElse("");

        // Find the recommended restaurant in our data
        Map<String, Object> matched = RestaurantTools.RESTAURANTS.stream()
            .filter(r -> output.toLowerCase()
                .contains(r.get("name").toString().toLowerCase()))
            .findFirst().orElse(null);

        int price = (int) matched.get("pricePerPerson");
        if (price <= EXPENSE_LIMIT) {
            return Judgment.builder()
                .status(JudgmentStatus.PASS)
                .reasoning("EUR " + price + " within EUR 50 limit")
                .checks(List.of(Check.pass("within-budget", ...)))
                .build();
        } else {
            return Judgment.builder()
                .status(JudgmentStatus.FAIL)
                .reasoning("EUR " + price + " exceeds EUR 50 limit")
                .checks(List.of(Check.fail("within-budget", ...)))
                .build();
        }
    }
}
```

<!--
SPEAKER (≈20s):
"The expense judge uses the same restaurant data the agent uses. Build once, measure with the same logic. If the agent recommends Tickets Bar at 75 euros, this judge catches it — deterministically, instantly, for free."
-->

---
layout: default
---

# Judge — LLM Judge (T2)

<div class="mt-4 text-sm">

Template method pattern — only fires when T0+T1 pass:

</div>

```java
public class RecommendationQualityJudge extends LLMJudge {

    @Override
    protected String buildPrompt(JudgmentContext context) {
        return String.format("""
            User goal: %s
            Agent output: %s

            Rate this recommendation:
            1. Does it clearly name a specific restaurant?
            2. Does it explain WHY this restaurant fits?
            3. Is it concise and actionable?

            Respond: GOOD: <reasoning>  or  POOR: <reasoning>
            """, context.goal(), context.agentOutput().orElse(""));
    }

    @Override
    protected Judgment parseResponse(String response, JudgmentContext context) {
        if (response.trim().toUpperCase().startsWith("GOOD")) {
            return Judgment.pass(response.substring(5).trim());
        }
        return Judgment.builder()
            .status(JudgmentStatus.FAIL)
            .reasoning(response.substring(5).trim()).build();
    }
}
```

<!--
SPEAKER (≈20s):
"The LLM judge uses a template method pattern: buildPrompt and parseResponse. The prompt is focused — three criteria, binary output. Simple for the LLM to evaluate. And it only fires if the deterministic judges pass first."
-->

---
layout: default
---

# Judge — Wiring the Cascade

```java
// Build three tiers
SimpleJury tier0 = SimpleJury.builder()
    .judge(new ExpensePolicyJudge())
    .votingStrategy(new MajorityVotingStrategy()).build();

SimpleJury tier1 = SimpleJury.builder()
    .judge(new DietaryComplianceJudge())
    .votingStrategy(new MajorityVotingStrategy()).build();

SimpleJury tier2 = SimpleJury.builder()
    .judge(new RecommendationQualityJudge(chatClientBuilder))
    .votingStrategy(new MajorityVotingStrategy()).build();

// Wire into cascaded jury
CascadedJury jury = CascadedJury.builder()
    .tier("T0-ExpensePolicy",    tier0, TierPolicy.REJECT_ON_ANY_FAIL)
    .tier("T1-DietaryCompliance", tier1, TierPolicy.ACCEPT_ON_ALL_PASS)
    .tier("T2-Quality",          tier2, TierPolicy.FINAL_TIER)
    .build();

// Evaluate
Verdict verdict = jury.vote(JudgmentContext.builder()
    .goal(goal).agentOutput(reply).build());
```

<!--
SPEAKER (≈30s):
"Three tiers, three policies. REJECT_ON_ANY_FAIL: if expense fails, stop immediately. ACCEPT_ON_ALL_PASS: if dietary passes, we could stop — but we escalate to quality. FINAL_TIER: the LLM judge always produces a verdict. The cascade minimizes token spend while maximizing evaluation coverage."
-->

---
layout: default
---

# Judge — Tier Policies

<div class="mt-6">

| Policy | Behavior | Use For |
|--------|----------|---------|
| `REJECT_ON_ANY_FAIL` | Any judge fails → STOP, reject | Hard constraints (budget) |
| `ACCEPT_ON_ALL_PASS` | All judges pass → STOP, accept | Structural checks (dietary) |
| `FINAL_TIER` | Always produces verdict | Semantic evaluation (quality) |

</div>

<v-click>

<div class="mt-8">

### Cascade Flow

```
T0: ExpensePolicy → FAIL? ──→ STOP (0 tokens)
                    PASS? ──→ escalate
T1: Dietary        → PASS? ──→ escalate (could stop, but want quality check)
                    FAIL? ──→ escalate
T2: Quality        → GOOD/POOR ──→ DONE (only now do we spend LLM tokens)
```

</div>

</v-click>

<!--
SPEAKER (≈30s):
"Three policies control the cascade flow. REJECT_ON_ANY_FAIL is the fail-fast gate — if the price is wrong, don't waste tokens on quality. ACCEPT_ON_ALL_PASS lets you short-circuit when structural checks are sufficient. FINAL_TIER always runs and always produces a verdict."
-->

---
layout: center
class: text-center
---

# Step 15: Trajectory

<div class="text-3xl mt-8 opacity-75">
Why did it fail that way?
</div>

<!--
SPEAKER (≈10s):
"Step 15 goes beyond pass/fail. The judge tells you WHAT failed. Trajectory tells you WHY — by analyzing the agent's behavior step by step."
-->

---
layout: default
---

# Trajectory — From Tool Calls to Semantic States

<div class="mt-4 text-sm">

Map every tool call to what the agent was trying to do:

</div>

<div class="mt-4 grid grid-cols-2 gap-6">

<div>

| Tool Call | Semantic State |
|-----------|---------------|
| `searchRestaurants` | SEARCH |
| `checkExpensePolicy` | CHECK_BUDGET |
| `checkDietaryOptions` | CHECK_DIETARY |
| `bookTable` | BOOK |

```java
private static final Map<String, SemanticState>
    TOOL_TO_STATE = Map.of(
      "searchRestaurants",   SEARCH,
      "checkExpensePolicy",  CHECK_BUDGET,
      "checkDietaryOptions", CHECK_DIETARY,
      "bookTable",           BOOK
    );
```

</div>

<div>

<v-click>

### Why semantic states?

* Tool names are implementation details
* Semantic states are **domain concepts**
* "SEARCH → SEARCH → SEARCH" means something different than "SEARCH → CHECK_BUDGET → BOOK"
* States enable pattern matching across runs

</v-click>

</div>

</div>

<!--
SPEAKER (≈30s):
"We classify every tool call into a semantic state. Not the tool name — the domain concept. This abstraction lets us reason about agent behavior at the right level. 'The agent searched three times' is more useful than 'searchRestaurants was called three times.'"
-->

---
layout: default
---

# Trajectory — Good vs Bad

<div class="mt-6 grid grid-cols-2 gap-6">

<div>

### Good Trajectory (efficient)

<div class="mt-2 p-4 bg-green-900/50 rounded-lg font-mono text-sm">
SEARCH → CHECK_BUDGET → CHECK_DIETARY → BOOK
</div>

<div class="mt-2 text-sm">

* **States**: 4
* **Loops**: 0
* **Efficiency**: 100%
* Every step moves forward

</div>

</div>

<div>

### Bad Trajectory (looping)

<div class="mt-2 p-4 bg-red-900/50 rounded-lg font-mono text-sm">
SEARCH → SEARCH → SEARCH → CHECK_BUDGET → BOOK
</div>

<div class="mt-2 text-sm">

* **States**: 5
* **Self-loops**: 2
* **Efficiency**: 60%
* **Hotspot**: SEARCH (3 visits, 2 self-loops)

</div>

</div>

</div>

<v-click>

<div class="mt-6 p-4 bg-yellow-50 dark:bg-yellow-900 rounded-lg text-sm">
<strong>The difference</strong>: The judge tells you the output was wrong. The trajectory tells you the agent searched three times before checking the budget — that's <strong>where</strong> to fix the prompt.
</div>

</v-click>

<!--
SPEAKER (≈30s):
"A good agent goes SEARCH, CHECK_BUDGET, CHECK_DIETARY, BOOK — four states, no loops, 100% efficiency. A bad agent searches three times before checking the budget. The judge tells you the output failed. The trajectory tells you WHERE the agent wasted time — that's actionable."
-->

---
layout: default
---

# Trajectory — Loop and Hotspot Detection

```java
// Detect self-loops: same state appearing consecutively
int selfLoopCount = 0;
for (int i = 0; i < sequence.size() - 1; i++) {
    if (sequence.get(i) == sequence.get(i + 1)) {
        selfLoopCount++;
        loops.add(String.format("%s called consecutively at positions %d-%d",
            sequence.get(i), i, i + 1));
    }
}

// Find hotspot: most-visited state
for (Map.Entry<SemanticState, Integer> entry : stateCounts.entrySet()) {
    if (entry.getValue() > maxCount) {
        maxCount = entry.getValue();
        hotspot = entry.getKey().name();
    }
}
```

<v-click>

<div class="mt-4">

### Efficiency Formula

```
efficiency = (total states - self-loops) / total states
```

<div class="mt-2 text-sm opacity-75">
100% = every step moves forward. Below 70% = the agent is stuck somewhere.
</div>

</div>

</v-click>

<!--
SPEAKER (≈30s):
"Self-loop detection: if the same state appears consecutively, that's a loop. Hotspot: which state gets visited most. Efficiency: total minus loops, divided by total. Simple math, powerful signal. Below 70% means your agent is stuck — and the hotspot tells you exactly where."
-->

---
layout: default
---

# Trajectory — Recording with ToolCallTracker

<div class="mt-4 text-sm">

Thread-local recording captures tool calls during the agent loop:

</div>

```java
public class ToolCallTracker {
    private static final ThreadLocal<List<String>> CALLS =
        ThreadLocal.withInitial(ArrayList::new);

    public static void record(String toolName) {
        CALLS.get().add(toolName);
    }

    public static List<String> getAndClear() {
        List<String> result = List.copyOf(CALLS.get());
        CALLS.get().clear();
        return result;
    }
}
```

<v-click>

<div class="mt-4">

### Each tool records itself:

```java
@Tool(description = "Search restaurants by neighborhood")
public List<Map<String, Object>> searchRestaurants(String neighborhood) {
    ToolCallTracker.record("searchRestaurants");
    // ... actual tool logic
}
```

</div>

</v-click>

<!--
SPEAKER (≈20s):
"Thread-local tracker. Each tool calls record() at the start. After the agent loop completes, getAndClear returns the ordered sequence and resets. Simple, zero overhead, thread-safe."
-->

---
layout: default
---

# Trajectory — The Complete Analysis

```java
public record TrajectoryAnalysis(
    List<SemanticState> sequence,           // ordered state sequence
    Map<SemanticState, Integer> stateCounts, // visits per state
    Map<String, Integer> transitionCounts,   // state→state transitions
    List<String> loops,                      // detected self-loops
    String hotspot,                          // most-visited state
    double efficiency                        // (total - loops) / total
) {}
```

<v-click>

<div class="mt-6">

### Usage in TrajectoryHandler:

```java
// After agent loop completes
List<String> toolCalls = ToolCallTracker.getAndClear();
TrajectoryAnalysis trajectory = classifier.classify(toolCalls);

// Display in Inspector state panel alongside verdict
statePanel.add("trajectory", trajectory.toMarkdown());
statePanel.add("verdict", verdict.toMarkdown());
```

</div>

</v-click>

<v-click>

<div class="mt-4 p-3 bg-blue-50 dark:bg-blue-900 rounded-lg text-sm">
<strong>Combined view</strong>: Inspector shows trajectory + verdict side-by-side. You see WHAT failed (verdict) and WHERE the agent went wrong (trajectory).
</div>

</v-click>

<!--
SPEAKER (≈20s):
"The complete analysis is a record with six fields. The Inspector shows it side-by-side with the verdict. You see the agent's path AND its score in one view."
-->

---
layout: default
---

# The Full Picture — Mark's Modules

<div class="mt-6 grid grid-cols-4 gap-3 text-center text-sm">
  <div class="p-3 rounded-lg border-2 border-blue-500 bg-blue-900/30">
    <div class="font-bold">05 Journal</div>
    <div class="text-xs mt-1">Record turns, tokens, cost to JSONL</div>
  </div>
  <div class="p-3 rounded-lg border-2 border-green-500 bg-green-900/30">
    <div class="font-bold">13 Workflow</div>
    <div class="text-xs mt-1">Deterministic → AI → Validate</div>
  </div>
  <div class="p-3 rounded-lg border-2 border-purple-500 bg-purple-900/30">
    <div class="font-bold">14 Judge</div>
    <div class="text-xs mt-1">3-tier cascade: cheap → LLM</div>
  </div>
  <div class="p-3 rounded-lg border-2 border-orange-500 bg-orange-900/30">
    <div class="font-bold">15 Trajectory</div>
    <div class="text-xs mt-1">Semantic states + efficiency</div>
  </div>
</div>

<v-click>

<div class="mt-6">

| Module | Question It Answers | Cost |
|--------|-------------------|------|
| Journal | What happened? | Zero — recording only |
| Workflow | Can I control what the LLM does? | Minimal — one focused LLM call |
| Judge | Is the output correct? | Cheap checks first, LLM only if needed |
| Trajectory | Why did it behave that way? | Zero — pure classification |

</div>

</v-click>

<v-click>

<div class="mt-4 p-4 bg-blue-50 dark:bg-blue-900 rounded-lg text-sm">
<strong>Cost theme</strong>: Every module minimizes LLM spend. Deterministic where possible, LLM only when reasoning is required.
</div>

</v-click>

<!--
SPEAKER (≈30s):
"Four modules, four questions, one theme: minimize LLM cost. Journal records for free. Workflow uses one focused LLM call instead of a multi-turn loop. Judge runs deterministic checks first. Trajectory classifies with pure Java. The LLM only fires when reasoning is truly needed."
-->

---
layout: default
---

# Dependencies

<div class="mt-6">

| Module | Library | Source |
|--------|---------|--------|
| 05 Journal | `journal-core` | Structured JSONL recording |
| 05 Journal | `workflow-core` | AgentLoopAdvisor (listener hooks) |
| 13 Workflow | `workflow-flows` | Workflow DSL: steps, gates |
| 14 Judge | `agent-judge-core` | Judge, Jury, CascadedJury, Verdict |
| 14 Judge | `agent-judge-llm` | LLMJudge base class |
| 15 Trajectory | `agent-judge-core` | Same — reuses judgment model |

</div>

<div class="mt-6 text-sm opacity-75 text-center">
All versions managed by <code>agentworks-bom</code> — one version property in the parent POM.
</div>

<!--
SPEAKER (≈15s):
"Six library dependencies across four modules. All managed by a single BOM. Bump one version, everything updates."
-->

---
layout: center
class: text-center
---

# Let's Build

<div class="text-3xl mt-8 opacity-75">
05-journal → 13-workflow → 14-judge → 15-trajectory
</div>

<div class="mt-8 text-lg">

```
cd agents/05-journal && ./mvnw spring-boot:run
```

</div>

<!--
SPEAKER (≈10s):
"Enough slides. Let's build. Start with the journal — once you can record, everything else has data to work with."
-->
