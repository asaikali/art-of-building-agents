# Business Meal Planner

Standalone business meal planning agent scaffold.

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
