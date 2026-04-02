# Step 12 — Wrap Path (agent-client + Claude Code)

A completely different way to get an agent: wrap an existing CLI agent instead of building one from scratch.

## What's new vs Steps 01-11

- **No ChatClient, no @Tool** — the agent loop runs inside Claude Code, not in our Java process
- **`agent-client`** — Spring component that wraps Claude Code (or Gemini CLI) as a managed service
- **Same domain** — Jarvis + Barcelona restaurants, but the data is injected via `append-system-prompt`
- **Zero agent code** — just configuration + `agentClient.run(goal)`

## The two paths

| | Build Path (Steps 01-11) | Wrap Path (Step 12) |
|---|---|---|
| Agent loop | Spring AI `ChatClient` + `ToolCallAdvisor` / `AgentLoopAdvisor` | Claude Code CLI (built-in loop) |
| Tools | Java `@Tool` methods | Claude Code's built-in tools (Read, Write, Bash, etc.) |
| Code you write | Handler, tools, prompt, advisors | Configuration only |
| Observability | agent-journal `JournalLoopListener` | agent-client `PhaseCapture` (tokens, cost, tool calls) |
| Control | Full — turn limits, stuck detection, custom advisors | Config — max-turns, allowed-tools, budget limits |

## Key files

| File | Purpose |
|------|---------|
| `WrapPathApplication.java` | Spring Boot app — injects `AgentClient.Builder`, runs a goal |
| `application.yml` | Claude Code config: model, YOLO mode, restaurant data via `append-system-prompt` |

## Prerequisites

- **Claude CLI** installed and on PATH (`claude --version`)
- Claude Max subscription or `ANTHROPIC_API_KEY` set

## Run it

```bash
cd agents/12-wrap-path
../../mvnw spring-boot:run
```

With a custom goal:

```bash
../../mvnw spring-boot:run -Dspring-boot.run.arguments="Find a quiet restaurant in Eixample for a client dinner, budget 100 EUR per person"
```

## How it works

```
WrapPathApplication
     │
     ▼
AgentClient.Builder  ◄── auto-configured by agent-claude
     │
     ▼
agentClient.run(goal)
     │
     ▼
Claude Code CLI (subprocess)
  ├── reads append-system-prompt (restaurant data + expense rules)
  ├── reasons about the goal
  └── returns recommendation
     │
     ▼
AgentClientResponse
  ├── getResult()        → agent's answer
  ├── isSuccessful()     → did it complete?
  └── getPhaseCapture()  → tokens, cost, tool calls
```

## Why this matters

- **Bring your own agent** — don't rewrite what already works
- **Same measurement** — `PhaseCapture` gives you the same token/cost data as `agent-journal`
- **Same judges** — evaluate wrap-path agents with the same judge infrastructure
- **Fast prototyping** — go from "I have a CLI tool" to "I have a Spring service" in minutes
