# 01 Intent Alignment

Phase 1 scaffold for the business meal planner.

## Purpose

This sample app will evolve into the intent-alignment phase that:

- extracts user constraints
- summarizes the user's intent
- asks for confirmation or corrections

## What's here

| File | Purpose |
|------|---------|
| `IntentAlignmentApplication.java` | Spring Boot entry point |
| `IntentAlignmentHandler.java` | Minimal `AgentHandler` backed by a bare `ChatClient` |
| `application.yml` | Local app and model configuration |

## Run it

```bash
cd business-meal-planner/01-intent-alignment
../../mvnw spring-boot:run
# Open http://localhost:8080
```
