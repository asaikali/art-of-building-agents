# Step 11 — ACP (Agent Communication Protocol)

Jarvis exposed as an ACP endpoint — the protocol IDEs use to talk to agents.

## What's new vs Steps 01-10

- **`@AcpAgent` + `@Prompt`** — annotation-based ACP agent (no Inspector scaffold)
- **Two transports**: WebSocket (default, for demos) or stdio (for IDE integration)
- **Same ChatClient + tools** under the hood — ACP is just a different protocol layer

## Key files

| File | Purpose |
|------|---------|
| `JarvisAcpAgent.java` | `@AcpAgent` with `@Initialize`, `@NewSession`, `@Prompt` handlers |
| `AcpApplication.java` | Spring Boot app — DI for ChatModel, transport selection |
| `RestaurantTools.java` | Same 4 tools as previous steps |

## Run it

### WebSocket (default — for testing and demos)

```bash
cd agents/11-acp
../../mvnw spring-boot:run
# ACP agent on ws://localhost:8083/acp
```

### Stdio (for IDE integration)

```bash
cd agents/11-acp
../../mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=stdio"
# Reads/writes JSON-RPC on stdin/stdout
```

## IDE Integration

### IntelliJ IDEA

Create `~/.jetbrains/acp.json`:

```json
{
  "agents": [{
    "name": "jarvis",
    "command": "java",
    "args": ["-jar", "/path/to/agents/11-acp/target/11-acp-0.0.1-SNAPSHOT.jar",
             "--spring.profiles.active=stdio"]
  }]
}
```

### Zed

Add to `settings.json`:

```json
{
  "agent": {
    "profiles": {
      "jarvis": {
        "command": "java",
        "args": ["-jar", "/path/to/11-acp.jar", "--spring.profiles.active=stdio"]
      }
    }
  }
}
```

## How it works

```
IDE/Client                    ACP Protocol                  Jarvis Agent
─────────                    ────────────                  ────────────
initialize    ─── JSON-RPC ──→   @Initialize
              ←──────────────   InitializeResponse.ok()

session/new   ─── JSON-RPC ──→   @NewSession
              ←──────────────   NewSessionResponse(sessionId)

session/prompt ── JSON-RPC ──→   @Prompt
                                   ChatClient + tools
                                   searchRestaurants → check → filter
              ←──────────────   PromptResponse.text("Teresa Carles...")
```

## ACP vs Inspector (Steps 01-10)

| Aspect | Inspector (Steps 01-10) | ACP (Step 11) |
|--------|------------------------|---------------|
| Protocol | HTTP + SSE | JSON-RPC (stdio or WebSocket) |
| UI | Inspector web UI | IDE (IntelliJ, Zed, VS Code) |
| Session | Managed by agent-core | Managed by ACP protocol |
| Discovery | Browser → localhost:8080 | IDE configuration |

## Why this matters

- **IDE integration** — bring your agent to where developers already work
- **Standard protocol** — ACP is supported by IntelliJ, Zed, VS Code
- **Same agent logic** — only the transport changes, not the business logic
- **Production path** — from workshop prototype to IDE plugin
