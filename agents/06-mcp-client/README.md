# Step 06b — MCP Client

Same Jarvis agent as Steps 03-05, but now the restaurant tools come from a remote MCP server instead of being compiled into this app. Tools are discovered dynamically at startup via the MCP protocol.

## What's new vs Step 05

- **No `RestaurantTools.java`** — tools are no longer in this module
- **`spring-ai-starter-mcp-client`** — auto-discovers tools from connected MCP servers
- **`ToolCallbackProvider`** — Spring auto-configures this from MCP server connections; injected into the handler like any other Spring bean
- **Streamable HTTP transport** — connects to the MCP server at `http://localhost:8081`

## What's the same

System prompt, `AgentLoopAdvisor`, turn limits — all identical to Steps 04-05. The only change is where tools come from.

## Key files

| File | Purpose |
|------|---------|
| `McpClientHandler.java` | Injects `ToolCallbackProvider` (from MCP server) instead of local `RestaurantTools` |
| `application.yml` | MCP client config: connects to `http://localhost:8081` via streamable-http |

## Run it

First, start the MCP server (Step 06a):

```bash
cd agents/06-mcp-server
../../mvnw spring-boot:run
```

Then, in another terminal, start the MCP client:

```bash
cd agents/06-mcp-client
../../mvnw spring-boot:run
# Open http://localhost:8080
```

On startup, you'll see in the logs:
```
MCP tools discovered: org.springframework.ai.mcp.SyncMcpToolCallbackProvider@...
```

## What to observe

- The **State panel** shows "Tools | from MCP server (localhost:8081)"
- Same conversation flow as Steps 03-05 — the user experience is identical
- The tools are served from a separate process (port 8081), discovered at startup

## New dependencies

| Artifact | Purpose |
|----------|---------|
| `org.springframework.ai:spring-ai-starter-mcp-client` | MCP client auto-configuration |

Both MCP SDK versions are pinned to 1.1.1 via the parent POM's `mcp-bom` import.

## Next step

Step 07 will add memory — the agent remembers context across conversations.
