# Inspector

The browser UI half of [scaffold](../README.md). A Vue 3 frontend for testing
any agent built on [agent-core](../agent-core/README.md), with chat, state, and
events for each session in a four-panel layout.

## Setup

```bash
cd scaffold/inspector
npm install
npm run dev
```

The inspector runs on `http://localhost:5173` and proxies `/api` requests to
the agent-core backend on `http://localhost:8080`.

## Usage

1. Start a Spring Boot app that depends on agent-core (e.g. any meal-agent
   module like `01-intent-alignment`).
2. Start the inspector with `npm run dev`.
3. Open **Chrome** at <http://localhost:5173>.
4. Create a new session and start chatting.

> **Use Chrome.** Other browsers have known issues with SSE streaming that cause
> the state and events panels to stop updating.

## Panels

- **Session Navigator** — list and create sessions (left rail, collapsible)
- **Chat** — message transcript with markdown rendering for assistant replies
- **State** — versioned agent state as rendered markdown, with revision browsing
- **Events** — append-only event log with expandable JSON payloads

All panels update in real time via Server-Sent Events.
