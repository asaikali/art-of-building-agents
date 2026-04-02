# Step 10a — Expense Policy A2A Agent (Server)

A standalone expense policy checker exposed via Google's Agent-to-Agent (A2A) protocol.

## What's new

- **A2A protocol** — `AgentCard` discovery at `/.well-known/agent-card.json` + JSON-RPC message endpoint
- **`spring-ai-a2a-server-autoconfigure`** — auto-configures A2A endpoints from `AgentCard` and `AgentExecutor` beans
- **Single-purpose agent** — only has `checkExpensePolicy` tool (separation of concerns)

## Key files

| File | Purpose |
|------|---------|
| `ExpensePolicyApplication.java` | `AgentCard` bean + `AgentExecutor` bean (DefaultAgentExecutor) |
| `ExpensePolicyTools.java` | `checkExpensePolicy` — same deterministic logic as Steps 03-09 |

## Run it

```bash
cd agents/10-a2a-expense
../../mvnw spring-boot:run
# Runs on port 8082
# Agent card: http://localhost:8082/.well-known/agent-card.json
```

## A2A endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /.well-known/agent-card.json` | Agent discovery — returns capabilities, skills, protocol version |
| `POST /` | JSON-RPC message endpoint — receives tasks from other agents |

## Architecture

```
spring-ai-a2a-server-autoconfigure
  ↓ auto-configures
AgentCard bean → /.well-known/agent-card.json
AgentExecutor bean → JSON-RPC endpoint
  ↓ delegates to
DefaultAgentExecutor → ChatClient → checkExpensePolicy tool
```

## Next step

See `10-a2a-client` — Jarvis discovers this agent and calls it via A2A protocol.
