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

## Stage 4: Fully Dynamic Splitpanes Layout

**You learn**: How to nest splitpanes so every panel is drag-resizable. How splitpanes events and sizing work. Integrating a collapsible panel within a splitpanes system.

**What we build**:
- Update `InspectorView.vue` — move the SessionNavigator *inside* the splitpanes layout so the full structure is: `[Navigator | Chat | [State / Events]]` where all dividers are draggable
- Update `SessionNavigator.vue` — accept `collapsed` as a prop (controlled by parent) instead of internal ref. Emit `toggle` event. Remove fixed CSS width — splitpanes controls sizing now. Content hides/shows based on `collapsed` prop.
- Handle splitpanes `@resize` event to track pane sizes so the Navigator auto-collapses when dragged small enough

**Result**: All four panels (Navigator, Chat, State, Events) are drag-resizable via splitpanes dividers. The Navigator still has a collapse toggle button, but its width is controlled by splitpanes.

**Files**: ~0 new files, ~2 modified (`InspectorView.vue`, `SessionNavigator.vue`)

---

## Stage 5: Composables, API Wiring, SSE + Polish

**You learn**: Vue composables (reusable reactive logic). `fetch()` API calls with TypeScript types. Server-Sent Events (EventSource API). Real-time reactive updates. Component cleanup (`onUnmounted`). Pop-out windows with `window.open()`.

**What we build**:
- `src/types.ts` — TypeScript interfaces matching the spec: `SessionMeta`, `Message`, `InspectorEvent`, `StateRevision`
- `src/composables/useSessionApi.ts` — fetch wrappers for all REST endpoints. Fail gracefully when no backend is running.
- `src/composables/useMarkdown.ts` — wraps `markdown-it` + `dompurify` in a composable
- `src/composables/useSse.ts` — `EventSource` wrapper returning `{ data, status, close }`. Auto-cleanup on unmount. Reconnection with backoff.
- Wire all components to use real API calls and SSE subscriptions
- Wire pop-out views with independent SSE subscriptions
- Edge cases: session not found (404), empty states, loading indicators

**Result**: Full spec-compliant frontend. Without a backend, shows graceful empty states. Point a running backend at `:8080`, and the Inspector shows live events, state updates, and chat — including in independent pop-out windows.

**Files**: ~4 new files, ~8 modified

---

## Summary

| Stage | What You Learn | What You See After |
|-------|---------------|-------------------|
| 1 | Vue+Vite+TS+Tailwind project structure | Styled "Hello Inspector" page with hot reload |
| 2 | Router, components, props, ref(), transitions | Header + collapsible sidebar, route navigation |
| 3 | Splitpanes, markdown rendering, component composition | Resizable Chat/State/Events panels with mock data |
| 4 | Nested splitpanes, prop-driven collapse, pane events | All 4 panels drag-resizable including Navigator |
| 5 | Composables, fetch, SSE, real-time updates, pop-outs | Live-updating Inspector ready for any backend |
