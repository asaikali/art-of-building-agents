# Scaffold

The reusable platform every workshop agent runs on. Two pieces:

- [`agent-core/`](agent-core/README.md) — Spring Boot backend that hosts an
  agent. Manages sessions, dispatches each user message to a pluggable handler,
  and streams the handler's replies, state, and events back to the UI.
- [`inspector/`](inspector/README.md) — Vue 3 browser UI for testing an agent
  and inspecting its internal state in real time.

## How this fits together

```
┌──────────────────────────┐         ┌──────────────────────────┐         ┌──────────────────────────┐
│ scaffold/inspector       │  HTTP   │ scaffold/agent-core      │  calls  │ your agent handler       │
│                          │  + SSE  │                          │  the    │                          │
│ Browser UI for testing   │ ──────► │ Spring Boot backend for  │ handler │ Whatever the agent       │
│ the agent. Chat with it  │         │ the UI. Holds sessions   │ ──────► │ should do. Plug into     │
│ and inspect its internal │ ◄────── │ and dispatches messages  │ ◄────── │ agent-core by            │
│ state and event stream   │  reply  │ to a pluggable handler   │  reply  │ implementing the         │
│ live.                    │ + state │ via strategy interfaces. │ + state │ AgentHandler interface.  │
└──────────────────────────┘         └──────────────────────────┘         └──────────────────────────┘
```

Scaffold knows nothing about any specific domain. An agent plugs in by
implementing the `AgentHandler` interface from agent-core and being on the
classpath of a Spring Boot app that depends on `agent-core`.

For an example of an agent built on this platform, see
[`../meal-agent/`](../meal-agent/README.md).

## Run it

The two pieces run as two processes during development.

In one terminal, start a Spring Boot app that uses agent-core (e.g. one of the
meal-agent modules):

```bash
cd meal-agent/<module>
../../mvnw spring-boot:run
```

In another terminal, start the inspector:

```bash
cd scaffold/inspector
npm install   # first time only
npm run dev
```

Then open <http://localhost:5173>. The inspector proxies `/api` calls to the
backend on port 8080.
