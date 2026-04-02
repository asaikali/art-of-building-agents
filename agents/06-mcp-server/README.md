# Step 06a — MCP Server

The restaurant tools, extracted into a standalone MCP server. No AI model dependency — this is a pure tool server that any MCP-compatible client can connect to.

## What's new vs Step 05

- **`spring-ai-starter-mcp-server-webmvc`** — exposes `@Tool`-annotated methods over the MCP protocol (Streamable HTTP)
- **No Spring AI model dependency** — this module doesn't call an LLM; it only serves tools
- **`MethodToolCallbackProvider`** — Spring AI's bridge that turns `@Tool` methods into MCP tool definitions

## Key files

| File | Purpose |
|------|---------|
| `McpServerApplication.java` | Boot app + `@Bean` that registers tools with the MCP server |
| `RestaurantTools.java` | Same 4 tools as Steps 03-05, now discoverable via MCP |
| `application.yml` | Port 8081, Streamable HTTP protocol, server name "restaurant-tools" |

## Run it

```bash
cd agents/06-mcp-server
../../mvnw spring-boot:run
```

The server starts on port 8081. You can verify it's running:

```bash
# MCP initialize handshake
curl -X POST http://localhost:8081/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"test","version":"1.0.0"}}}'
```

## Why this matters

Tools live outside the agent. The MCP server can serve multiple agents. Add new tools without redeploying any agent.

## New dependencies

| Artifact | Purpose |
|----------|---------|
| `org.springframework.ai:spring-ai-starter-mcp-server-webmvc` | MCP server over Streamable HTTP |

## Next

See `06-mcp-client` — the Jarvis agent that discovers these tools dynamically at startup.
