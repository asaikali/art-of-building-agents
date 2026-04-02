# Step 08 — Human-in-the-Loop

Same agent as Steps 04-07, but now the agent can pause mid-loop to ask the user for clarification. Instead of guessing when requirements are ambiguous, it asks.

## What's new vs Steps 04-07

- **`AskUserQuestionTool`** — from `spring-ai-agent-utils`. The LLM calls this tool when it needs user input (missing details, ambiguous requirements)
- **`InspectorQuestionHandler`** — bridges the blocking tool with the Inspector's web UI using `CompletableFuture` handoff
- **Re-entrant handler** — when the user answers a question, `onMessage()` detects the pending question and completes it instead of starting a new agent loop
- **Updated system prompt** — instructs the agent to ask for clarification instead of guessing

## Key files

| File | Purpose |
|------|---------|
| `HumanInTheLoopHandler.java` | Handler with `AskUserQuestionTool` + `CompletableFuture` bridge to web UI |
| `RestaurantTools.java` | Same 4 tools as Steps 03-07 |

## Run it

```bash
cd agents/08-human-in-the-loop
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to try

1. **Vague request** — "I need a restaurant for dinner" → the agent should ask about neighborhood, budget, dietary needs, party size
2. **Complete request** — "Eixample, 4 people, 30 euros, vegetarian" → the agent proceeds without asking
3. **Partial request** — "Restaurant in Eixample for 4 people" → the agent asks about budget and dietary needs

## How the CompletableFuture bridge works

```
User sends message → onMessage() starts agent loop
                      ↓
                LLM calls AskUserQuestionTool
                      ↓
                InspectorQuestionHandler:
                  1. Posts question as ASSISTANT message (visible in chat)
                  2. Registers CompletableFuture for this session
                  3. Blocks on future.get()
                      ↓
User sees question in chat, sends answer
                      ↓
onMessage() called again:
  - Detects pendingAnswer for this session
  - Completes the future with user's text
  - Returns immediately (no new loop)
                      ↓
Original loop resumes with the answer
  - LLM gets tool result with user's answer
  - Continues planning with the new info
```

## Why this matters

- Agents that guess wrong waste time and erode trust
- Asking for clarification is cheaper than redoing work
- The `CompletableFuture` pattern is a general solution for bridging blocking tools with async UIs

## New dependencies

| Artifact | Purpose |
|----------|---------|
| `org.springaicommunity:spring-ai-agent-utils` | AskUserQuestionTool + QuestionHandler |

Version managed by `agentworks-bom` in the parent POM.

## Next step

Step 09 will add sub-agents — delegating restaurant research to a specialized sub-agent via TaskTool.
