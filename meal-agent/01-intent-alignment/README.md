# 01 Intent Alignment

Turn a messy natural-language request into confirmed, structured meal requirements.

## What this module teaches

- **Structured extraction** — use `ChatClient.entity()` to extract typed data from free text
- **Multi-turn conversation** — requirements accumulate across turns, the model preserves
  existing data unless the user explicitly corrects it
- **Deterministic status transitions** — Java code (not the model) decides whether to
  gather more info, ask for confirmation, or proceed
- **Separation of concerns** — the extractor, assessor, and composer each have one job

## Architecture

```
User message
  → RequirementsExtractor (LLM: merge message into UserRequirements)
  → RequirementsAssessor  (Java: check hard gates — date, time, party size)
  → determineStatus       (Java: gathering → confirming → confirmed)
  → ReplyComposer         (LLM: write a natural response for the current status)
```

Key classes:
- `RequirementsAligner` — orchestrates the 3-step pipeline
- `RequirementsExtractor` — LLM call that returns `UserRequirements` via `.entity()`
- `RequirementsAssessor` — deterministic missing-field check + LLM follow-up suggestion
- `ReplyComposer` — LLM call that writes the reply based on alignment status
- `JarvisAgentHandler` — wires the pipeline into the agent-core session

## Key design decisions

- **Confirmation is deterministic.** The model doesn't decide if the user confirmed — Java
  detects that requirements are unchanged after extraction (`isUnchanged`).
- **Hard gates are code, soft suggestions are model.** Date, time, and party size are
  checked with Java. Follow-up questions ("ask about cuisine?") are model-generated.
- **JSON is the context format.** `UserRequirements` is serialized to JSON for the model.

## Running and testing

See [meal-agent/README.md](../README.md#run-a-module) for run and test commands.
