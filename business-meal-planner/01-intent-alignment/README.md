# 01 Intent Alignment

Phase 1 of the business meal planner. This sample turns a messy user request into a
confirmed planning artifact before any restaurant search or booking happens.

## Purpose

This module now demonstrates:

- turning a messy request into structured event requirements and attendee inputs
- storing that artifact in session state
- using deterministic rules to decide whether to clarify or confirm
- rendering the artifact into markdown for the inspector
- looping on confirmation, correction, or clarification

## What's here

| File | Purpose |
|------|---------|
| `IntentAlignmentApplication.java` | Spring Boot entry point in `com.example.jarvis` |
| `JarvisAgentHandler.java` | Single agent handler wired into `agent-core` |
| `RequirementsAlignmentLoop.java` | Main phase-one loop: opener handling, one model call, deterministic status, deterministic reply |
| `RequirementsExtractor.java` | Single `ChatClient` call that extracts or updates the planning context JSON |
| `JarvisAgentContext.java` | The session context object for this module |
| `UserRequirements.java` | The captured user requirements aggregate for this module |
| `Meal.java` | Shared meal facts captured during intent alignment |
| `Attendee.java` | Per-person constraints captured during intent alignment |
| `application.yml` | Local app and model configuration |

## Run it

```bash
cd business-meal-planner/01-intent-alignment
../../mvnw spring-boot:run
# Open http://localhost:8080
```
