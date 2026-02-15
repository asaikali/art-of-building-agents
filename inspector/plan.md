# Inspector Frontend — 5 Stages

## Context

The Inspector is the workshop's observability tool — a Vue 3 frontend that talks to `localhost:8080`. The backend will be a multi-module Maven project built separately (shared components + swappable agent modules). The frontend doesn't care which agent is behind the port — it just renders what it gets.

These 5 stages build the frontend incrementally. Each stage teaches a specific Vue/frontend concept. We use hardcoded mock data so no backend is needed to follow along. Later, when the backend exists, the frontend will just work.

All work happens in `inspector/inspector-frontend/`.

---

## Stage 1: Project Scaffolding + Tailwind

**You learn**: How a Vue 3 + Vite + TypeScript project is structured. How Tailwind CSS v4 works. What each config file does.

**What we build**:
- `package.json` — dependencies (vue, vite, tailwindcss v4, typescript)
- `vite.config.ts` — Vue plugin, proxy `/sessions` to `localhost:8080`, `@` alias
- `tsconfig.json`, `tsconfig.app.json`, `tsconfig.node.json` — TypeScript config
- `env.d.ts` — Vite/Vue type declarations
- `index.html` — entry HTML
- `src/main.ts` — creates and mounts the Vue app
- `src/style.css` — Tailwind v4 import (`@import "tailwindcss"`)
- `src/App.vue` — a simple "Hello Inspector" page styled with Tailwind classes

**Result**: `npm install && npm run dev` opens a styled page at `localhost:5173`. You can see the dev server, hot reload, and Tailwind utility classes in action.

**Files**: ~9 new files

---

## Stage 2: Vue Router + App Shell (Header + Collapsible Navigator)

**You learn**: Vue Router (route definitions, `<router-view>`, `<router-link>`, route params). Vue components with props. Reactive state with `ref()`. Conditional rendering with `v-if`/`v-show`. Tailwind layout (flexbox, sticky positioning, transitions).

**What we build**:
- `src/router/index.ts` — routes: `/sessions/:id`, `/sessions/:id/chat`, `/sessions/:id/state`, `/sessions/:id/events`
- `src/components/AppHeader.vue` — sticky top bar showing agent name, session ID, status indicators (all hardcoded for now)
- `src/components/SessionNavigator.vue` — collapsible left rail (280px / 56px), `+ New Chat` button, hardcoded session list, `collapsed` ref toggles width
- `src/views/InspectorView.vue` — composes header + navigator + placeholder content area
- `src/views/ChatPopout.vue`, `StatePopout.vue`, `EventsPopout.vue` — header + placeholder text
- Update `src/App.vue` to use `<router-view />`
- Update `src/main.ts` to install router

**Result**: Navigate to `/sessions/1`, see sticky header and collapsible sidebar. Click sessions in the list to change the route param. Pop-out routes render their own pages.

**Files**: ~8 new files, ~2 modified

---

## Stage 3: Splitpanes Layout + Panel Components (Static)

**You learn**: How `splitpanes` works for resizable panels. Component composition — passing props and slots. Markdown rendering with `markdown-it` + `dompurify`. The `@tailwindcss/typography` prose classes.

**What we build**:
- Install `splitpanes`, `markdown-it`, `dompurify`, `@tailwindcss/typography`
- `src/components/ChatPanel.vue` — message list with hardcoded user/assistant messages, input box at bottom, role-based styling (user = right/blue, assistant = left/gray)
- `src/components/StateViewer.vue` — renders hardcoded markdown via `markdown-it`, sanitized with `dompurify`, displayed in `prose` container. Revision nav buttons (prev/next) — non-functional, just UI.
- `src/components/EventsViewer.vue` — list of hardcoded events, each with timestamp + message + expandable JSON data (click to toggle `<pre>` block)
- Update `InspectorView.vue` — replace placeholder content with splitpanes layout: `[Navigator | Chat | [State / Events]]`

**Result**: Full 4-panel layout. Drag dividers to resize panels. See rendered markdown in state viewer. Expand/collapse JSON in events viewer. Chat shows styled message bubbles.

**Files**: ~3 new files, ~2 modified

---

## Stage 4: Composables + API Wiring (Mock-Ready)

**You learn**: Vue composables (reusable reactive logic). `fetch()` API calls with TypeScript types. Reactive data flow — how API data flows into components. `watch()` and `onMounted()` lifecycle. How the frontend will connect to the real backend.

**What we build**:
- `src/types.ts` — TypeScript interfaces matching the spec: `SessionMeta`, `Message`, `Event`, `StateRevision`
- `src/composables/useSessionApi.ts` — functions: `createSession()`, `listSessions()`, `getSessionMeta()`, `sendMessage()`, `getMessages()`, `getStateRevision()`. All call `fetch('/sessions/...')`. They'll fail gracefully when no backend is running.
- `src/composables/useMarkdown.ts` — wraps `markdown-it` + `dompurify` in a composable
- Wire `SessionNavigator.vue` — try to load sessions from API, fall back to "No sessions — click + New Chat". `+ New Chat` calls `createSession()`.
- Wire `ChatPanel.vue` — load messages from API on mount, send via API. Props: `sessionId`.
- Wire `StateViewer.vue` — revision browsing via `getStateRevision()` API calls. Track `currentRev` / `latestRev`.
- Wire `InspectorView.vue` — load session meta on mount, pass data to header. Watch route param changes to re-load.

**Result**: The app is "wired but quiet" — all the API plumbing is in place. When a backend appears on `:8080`, everything lights up. Without a backend, graceful empty states show.

**Files**: ~3 new files, ~5 modified

---

## Stage 5: SSE Subscriptions + Live Updates + Polish

**You learn**: Server-Sent Events (EventSource API). Real-time reactive updates. Component cleanup (`onUnmounted`). Connection status management. Pop-out windows with `window.open()`.

**What we build**:
- `src/composables/useSse.ts` — `EventSource` wrapper returning `{ data, status, close }`. Handles open/error/message events. Auto-cleanup on unmount. Reconnection with backoff.
- Wire `StateViewer.vue` — subscribe to `/sessions/{id}/state/stream`, render incoming markdown live. Toggle between "following latest" and "browsing history" modes.
- Wire `EventsViewer.vue` — subscribe to `/sessions/{id}/events/stream`, append events to list. Auto-scroll to bottom on new events.
- Wire `AppHeader.vue` — show live SSE connection status (green/yellow/red dot), live rev count, live event count. Add pop-out buttons that call `window.open()`.
- Wire pop-out views (`ChatPopout.vue`, `StatePopout.vue`, `EventsPopout.vue`) — load session meta, embed real components with independent SSE subscriptions.
- Edge cases: session not found (404 display), empty states, loading indicators.
- Visual polish: consistent colors, splitpane gutter styling, min panel widths.

**Result**: Full spec-compliant frontend. Point a running backend at `:8080`, and the Inspector shows live events, state updates, and chat — including in independent pop-out windows.

**Files**: ~1 new file, ~8 modified

---

## Summary

| Stage | What You Learn | What You See After |
|-------|---------------|-------------------|
| 1 | Vue+Vite+TS+Tailwind project structure | Styled "Hello Inspector" page with hot reload |
| 2 | Router, components, props, ref(), transitions | Header + collapsible sidebar, route navigation |
| 3 | Splitpanes, markdown rendering, component composition | Full 4-panel resizable layout with mock data |
| 4 | Composables, fetch API, TypeScript types, watchers | API-wired app with graceful empty states |
| 5 | SSE (EventSource), real-time updates, pop-outs | Live-updating Inspector ready for any backend |
