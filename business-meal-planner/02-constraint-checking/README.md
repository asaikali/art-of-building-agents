# 02 Constraint Checking

Phase 2 scaffold for the business meal planner.

## Purpose

This sample app will evolve into the constraint-checking phase that:

- maps constraints to checking strategies
- demonstrates deterministic checks
- demonstrates hybrid and LLM-backed checks

## What's here

| File | Purpose |
|------|---------|
| `ConstraintCheckingApplication.java` | Spring Boot entry point |
| `ConstraintCheckingAgentHandler.java` | Minimal `AgentHandler` backed by a bare `ChatClient` |
| `application.yml` | Local app and model configuration |

## Run it

```bash
cd business-meal-planner/02-constraint-checking
../../mvnw spring-boot:run
# Open http://localhost:8080
```
