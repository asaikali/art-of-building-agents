# Business Meal Planner — Project Summary

## Purpose

The business meal planner is the first thing taught in the workshop. Students build a complete
AI agent from first principles, one phase at a time. Each phase introduces one architectural
concept in isolation — no framework magic, just Java, Spring Boot, and raw ChatClient calls.
Once students complete this end-to-end run, the workshop pivots to Spring AI features showing
how frameworks abstract the patterns they just built by hand.

## Phases

| Phase | Name | Concept | Status |
|-------|------|---------|--------|
| 01 | Intent Alignment | Structured extraction, confirmation loops, deterministic status transitions | Implemented |
| 02 | Constraint Checking | Deterministic, hybrid, and LLM-backed validation strategies | Scaffold |
| 03 | Restaurant Planning | Bounded search loops, tool calling, candidate filtering and ranking | Scaffold |
| 04 | Decision Support | Comparison, trade-off analysis, user-guided selection | Scaffold |
| 05 | Booking | Commitment, execution, failure handling with fallback | Scaffold |

## Domain

A user describes a business meal (date, attendees, dietary needs, budget, location constraints).
The agent progressively refines requirements, validates constraints, finds restaurants, helps the
user choose, and books.

## Domain Model

- **UserRequirements** — top-level aggregate
  - **Meal** — date, time, party size, meal type, purpose, budget per person, noise level, cuisine preferences, additional requirements
  - **List\<Attendee\>** — name, origin, departure time, travel mode, max travel time, dietary constraints
- Enums: `MealType`, `NoiseLevel`, `TravelMode`, `DietaryConstraint` — all with `@JsonCreator` factories for LLM resilience

## Shared Infrastructure (agent-core)

All phases plug into `components/agent-core`, a lightweight LLM-agnostic framework providing:

- **Session management** — per-session typed context via `AgentContext`
- **Chat** — message history (USER/ASSISTANT roles)
- **State** — versioned markdown snapshots streamed via SSE
- **Events** — append-only structured log streamed via SSE
- **REST API** — `/api/sessions`, messages, state, events endpoints

The sole extension point is `AgentHandler`: implement `getName()`, `getInitialAssistantMessage()`,
and `onMessage(session, message)`. Interact with the system through the `Session` facade:
`session.reply()`, `session.updateState()`, `session.logEvent()`, `session.getOrCreateContext()`.

## Phase 1 Detail (Intent Alignment)

The only fully implemented phase. Demonstrates a four-step alignment pipeline orchestrated
by `RequirementsAligner`:

1. **Extract** (`RequirementsExtractor`) — ChatClient with system prompt, returns `UserRequirements` via `.entity()` structured output
2. **Assess** (`RequirementsAssessor`) — deterministic checks for hard gates (date, time, party size) plus model-based optional follow-up suggestions
3. **Status Decision** (in `RequirementsAligner`) — deterministic Java logic, not model-based. Three states: `WAITING_FOR_CLARIFICATION` → `WAITING_FOR_CONFIRMATION` → `REQUIREMENTS_CONFIRMED`. Confirmation detected by equality check (requirements unchanged between turns)
4. **Reply** (`ReplyComposer`) — ChatClient generates natural language response appropriate to current status

Key design choices:

- Confirmation is deterministic (equality check), not model-guessed
- Hard gates are Java logic; soft suggestions are model-delegated
- JSON is the context format (UserRequirements serialized for model visibility)
- Enums handle LLM quirks (blank strings, wrong casing) via custom deserializers

## Inspector

A Vue 3 frontend at `inspector/` provides real-time observability with four panels:

- **Session Navigator** — list and create sessions
- **Chat Panel** — message transcript with input
- **State Viewer** — rendered markdown with revision browsing
- **Events Viewer** — sequential event log with expandable JSON payloads

Connects to agent-core via SSE. Handles backend restarts gracefully.

## Tech Stack

- Java 21, Spring Boot 4.1.0-M2, Spring AI 2.0.0-M3
- Maven multi-module with Spotless (Google Java Format)
- In-memory storage (no database, resets on restart)
