# Step 10b — A2A Client (Jarvis with Remote Expense Agent)

Jarvis delegates expense policy checks to the remote A2A agent from Step 10a, while keeping restaurant search, dietary checks, and booking as local tools.

## What's new vs Step 09

- **`RemoteExpenseAgent`** — discovers the Expense Policy Agent via `AgentCard` at startup, sends queries via A2A protocol
- **A2A Java SDK** — `io.a2a.A2A.getAgentCard()` for discovery, `Client.builder()` for JSON-RPC messaging
- **Mixed local + remote tools** — search/dietary/book are local; expense check is remote

## Key files

| File | Purpose |
|------|---------|
| `A2AClientHandler.java` | Jarvis coordinator with local + remote tools |
| `RemoteExpenseAgent.java` | A2A client — `@Tool checkExpensePolicyRemote` calls remote agent |
| `RestaurantTools.java` | Local tools: search, dietary, book (no expense policy) |

## Run it

```bash
# Terminal 1: Start the A2A expense agent
cd agents/10-a2a-expense
../../mvnw spring-boot:run
# Runs on port 8082

# Terminal 2: Start Jarvis
cd agents/10-a2a-client
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## How it works

```
User: "Find a restaurant in Eixample, 30 EUR/person, 4 guests, vegetarian"
  ↓
Jarvis (port 8080):
  - searchRestaurants("Eixample") → [Cervecería Catalana, Teresa Carles]
  - checkExpensePolicyRemote("Is 22 EUR within policy for 4 guests?")
      ↓ A2A protocol (JSON-RPC over HTTP)
      Expense Policy Agent (port 8082):
        - checkExpensePolicy(22, 4) → {withinPolicy: true, ...}
        - Returns: "22 EUR per person is within the 50 EUR limit"
      ↓
  - checkDietaryOptions("Teresa Carles", "vegetarian") → {hasOptions: true}
  - Presents: "Teresa Carles (22 EUR, vegetarian, Eixample) ✓"
```

## What to observe

- **Two processes** — expense agent on 8082, Jarvis on 8080
- Response takes slightly longer (cross-process A2A call)
- The State panel shows "Remote Agent | Expense Policy (A2A, port 8082)"

## Why this matters

- **Protocol-based composition** — agents communicate via a standard protocol, not direct method calls
- **Independent deployment** — the expense agent can be updated, scaled, or replaced independently
- **Discovery** — Jarvis finds the agent via `/.well-known/agent-card.json`
- **Interoperability** — any A2A-compatible agent (Python, TypeScript, etc.) could replace either side
