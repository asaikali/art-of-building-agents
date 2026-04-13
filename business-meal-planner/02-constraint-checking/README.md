# 02 Constraint Checking

Define how each user requirement can be checked against a restaurant candidate.

## What this module teaches

- **Three types of checks** — deterministic (pure Java), hybrid (Java evidence + LLM
  judgment), and LLM-as-judge (qualitative assessment from metadata)
- **Evidence gathering before judgment** — the hybrid dietary check filters the menu to
  main courses in Java, then asks the model to judge suitability
- **Strongly typed results** — each check returns a record with a status enum and rationale
- **Orchestration without scoring** — `RestaurantCandidateCheckService` runs all checks and
  returns a typed aggregate, but doesn't decide what's blocking

## Architecture

```
constraints/
  deterministic/
    budget/     BudgetPerPersonCheck    — compare budget to restaurant price range
    noise/      NoiseLevelCheck         — ranked comparison (QUIET ≤ MODERATE ≤ LOUD)
    travel/     TravelTimeCheck         — fake neighborhood matrix + mode adjustments
  hybrid/
    dietary/    DietarySuitabilityCheck  — Java filters menu to mains, LLM judges fit
  llmjudge/
    suitability/ MealSuitabilityCheck   — LLM evaluates overall venue fit from metadata

RestaurantCandidateCheckService          — runs all 5 checks, returns aggregate result
```

## Key design decisions

- **Checks have small input shapes.** `NoiseLevelCheck` takes a `NoiseLevel` and a
  restaurant ID — not `UserRequirements`. The orchestration service adapts from the
  higher-level model.
- **Menu filtering is Java, not prompt.** The dietary check strips appetizer/dessert/side
  sections before sending to the LLM, so the model focuses on main courses.
- **PASS/FAIL/MAYBE/UNSURE** — each status has clear meaning. FAIL is a hard stop. MAYBE
  means the value falls in a range. UNSURE means not enough evidence.

## Run it

```bash
cd business-meal-planner/02-constraint-checking
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## Tests

```bash
# Unit tests — deterministic checks + orchestration (no API key needed)
../../mvnw test

# Integration tests — hybrid and LLM checks (requires API key)
../../mvnw test -Dgroups=integration
```
