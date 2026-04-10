# Business Meal Planner

Parent module for the business meal planner teaching samples.

## Specification

See [SPEC.md](SPEC.md) for the phased product and architecture spec.

## Planned Module Layout

This parent module contains one child module per teaching phase:

- `01-intent-alignment`
- `02-constraint-checking`
- `03-restaurant-planning`
- `04-decision-support`
- `05-booking`

## Modules

| Module | Purpose |
|--------|---------|
| `01-intent-alignment` | Confirm the user's intent and summarize constraints |
| `02-constraint-checking` | Demonstrate deterministic, hybrid, and LLM-backed checks |
| `03-restaurant-planning` | Search and check restaurant candidates in a bounded loop |
| `04-decision-support` | Help the user inspect, compare, and choose options |
| `05-booking` | Commit to a selected option and complete booking steps |

## Run A Phase

```bash
cd business-meal-planner/01-intent-alignment
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## Current state

Each child module is currently a minimal standalone Spring Boot agent scaffold based on
the original `business-meal-planner` sample:

- no system prompt
- no tools
- no explicit agent loop
- no business-meal domain logic yet
