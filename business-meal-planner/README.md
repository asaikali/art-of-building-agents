# Business Meal Planner

Standalone business meal planning agent scaffold.

## Specification

See [SPEC.md](SPEC.md) for the phased product and architecture spec.

## Planned Module Layout

This scaffold is intended to become a parent Maven module with one child module per
teaching phase:

- `01-intent-alignment`
- `02-constraint-checking`
- `03-restaurant-planning`
- `04-decision-support`
- `05-booking`

## What's here

| File | Purpose |
|------|---------|
| `BusinessMealPlannerApplication.java` | Spring Boot entry point |
| `BusinessMealPlannerHandler.java` | Minimal `AgentHandler` backed by a bare `ChatClient` |
| `application.yml` | Local app and model configuration |

## Run it

```bash
cd business-meal-planner
../mvnw spring-boot:run
# Open http://localhost:8080
```

## Current state

This is only the starting scaffold:

- no system prompt
- no tools
- no explicit agent loop
- no dinner-planning domain logic yet

It is intended to evolve into the end-to-end dinner planning agent.
