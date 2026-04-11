# 01 Intent Alignment

Phase 1 of the business meal planner. This sample turns a messy user request into a
confirmed planning artifact before any restaurant search or booking happens.

## Purpose

This module now demonstrates:

- extracting intent and explicit constraints
- inferring reasonable business-meal defaults
- surfacing missing information and assumptions
- summarizing understanding back to the user
- looping on confirmation, correction, or clarification

## What's here

| File | Purpose |
|------|---------|
| `IntentAlignmentApplication.java` | Spring Boot entry point in `com.example.jarvis` |
| `JarvisHandler.java` | Single agent handler wired into `agent-core` |
| `IntentAlignmentConversationService.java` | Turn orchestration for initial, confirm, correct, and clarify flows |
| `ChatClientIntentAlignmentModelClient.java` | Two `ChatClient` calls: plan extraction and user-facing summary |
| `IntentAlignmentMarkdownRenderer.java` | Renders the required phase-one Markdown artifact |
| `BusinessMealRequirements.java` | Alignment-owned requirements object |
| `IntentAlignmentSessionStore.java` | Module-local per-session plan memory |
| `application.yml` | Local app and model configuration |

## Run it

```bash
cd business-meal-planner/01-intent-alignment
../../mvnw spring-boot:run
# Open http://localhost:8080
```
