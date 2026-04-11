# 03 Restaurant Planning

Phase 3 scaffold for the business meal planner.

## Purpose

This sample app will evolve into the restaurant-planning phase that:

- searches for candidate restaurants
- checks candidate fit against constraints
- produces a shortlist or failure summary

## What's here

| File | Purpose |
|------|---------|
| `RestaurantPlanningApplication.java` | Spring Boot entry point |
| `RestaurantPlanningAgentHandler.java` | Minimal `AgentHandler` backed by a bare `ChatClient` |
| `application.yml` | Local app and model configuration |

## Run it

```bash
cd business-meal-planner/03-restaurant-planning
../../mvnw spring-boot:run
# Open http://localhost:8080
```
