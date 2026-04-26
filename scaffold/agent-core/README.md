# Agent Core

The Spring Boot backend half of [scaffold](../README.md). Hosts an agent: holds
the session, takes incoming user messages, dispatches them to the agent's logic,
and streams replies, state updates, and events back out to the inspector via
Server-Sent Events.

Agent-core does not know what your agent *does* — it just runs it. The agent's
logic plugs in through the `AgentHandler` strategy interface.

## How an agent plugs in

Implement [`AgentHandler`](src/main/java/com/example/agent/core/chat/AgentHandler.java)
and expose it as a Spring bean. That's it.

```java
@Component
public class MyAgent implements AgentHandler {

  @Override
  public String getName() {
    return "MyAgent";
  }

  @Override
  public String getInitialAssistantMessage() {
    return "Hi! What can I help you with?";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    // Do whatever the agent does. Use the Session facade to talk back.
    session.reply("You said: " + message.text());
    session.logEvent("user-said", Map.of("text", message.text()));
    session.updateState("# State\n\nLast message: " + message.text());
  }
}
```

`onMessage` is called every time a USER-role message arrives at the chat
endpoint. The handler decides what to do; agent-core takes care of the wiring.

## What the Session facade gives you

[`Session`](src/main/java/com/example/agent/core/session/Session.java) is the one
object an agent talks to. It exposes:

- `reply(text)` / `appendMessage(role, text)` — append assistant messages to the
  conversation. The inspector sees them immediately.
- `logEvent(msg, data)` — append an entry to a per-session, append-only event
  log. Streamed live to the inspector's Events panel.
- `updateState(markdown)` — publish a versioned snapshot of the agent's
  user-visible state as a markdown string. Streamed live to the State panel.
- `getOrCreateContext(Class, factory)` — typed working memory the agent owns
  from turn to turn. Implement [`AgentContext`](src/main/java/com/example/agent/core/session/AgentContext.java)
  in your own model class.

These three streams (chat, events, state) plus the typed context are the only
ways the agent communicates outward — keeping the strategy interface small and
the inspector's view consistent across agents.

## REST and SSE endpoints

Provided by agent-core, mounted under `/api/`:

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/sessions` | Create a session |
| `GET` | `/api/sessions` | List sessions |
| `GET` | `/api/sessions/{id}/meta` | Session metadata |
| `GET` | `/api/sessions/{id}/messages` | Chat history |
| `POST` | `/api/sessions/{id}/messages` | Append a message (USER messages trigger the handler) |
| `GET` | `/api/sessions/{id}/events/stream` | SSE stream of events |
| `GET` | `/api/sessions/{id}/state/stream` | SSE stream of state revisions |
| `GET` | `/api/sessions/{id}/state/rev/{rev}` | Specific state revision |
| `GET` | `/api/heartbeat/stream` | SSE heartbeat with agent name + timestamp |

The inspector consumes these endpoints. Other clients can too.

## Packages

Each sub-package has a `package-info.java` with a conceptual overview. In short:

- `chat` — `AgentHandler` strategy interface, `AgentMessage`, REST endpoint that
  drives the handler.
- `session` — `Session` facade, lifecycle management, `AgentContext` marker.
- `event` — append-only event log + SSE stream.
- `state` — versioned markdown state snapshots + SSE stream.
- `sse` — generic broadcaster used by the event and state streams.
- `heartbeat` — heartbeat SSE the inspector uses to detect a live backend.
- `web` — global exception handling and SPA forwarder for production hosting.
- `json` — JSON serialization helper used by handlers.

