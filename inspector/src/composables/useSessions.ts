// Composable for managing the sessions list.
// Singleton — all consumers share the same reactive state.
// Fetches sessions from the backend and supports creating new ones.
// Tracks "stale" sessions that survived a backend restart (read-only).
// Caches session data (messages, events, state) so stale sessions remain viewable.

import { ref } from 'vue'
import { apiGet, apiPost } from '@/composables/useApi'
import type {
  AgentSessionMeta,
  AgentMessage,
  AgentEvent,
  AgentStateRevision,
  CreateSessionRequest,
} from '@/types'

// ---------------------------------------------------------------------------
// Stale session snapshot cache
// When sessions become stale (backend restarted), we snapshot the in-memory
// data so the user can navigate back and still see chat history, events, and
// state revisions for read-only sessions.
// ---------------------------------------------------------------------------
export interface SessionSnapshot {
  messages: AgentMessage[]
  events: AgentEvent[]
  stateRevisions: AgentStateRevision[]
}

const snapshotCache = new Map<number, SessionSnapshot>()

function cacheSessionData(
  sessionId: number,
  data: { messages?: AgentMessage[]; events?: AgentEvent[]; stateRevisions?: AgentStateRevision[] },
) {
  const existing = snapshotCache.get(sessionId) ?? { messages: [], events: [], stateRevisions: [] }
  if (data.messages) existing.messages = [...data.messages]
  if (data.events) existing.events = [...data.events]
  if (data.stateRevisions) existing.stateRevisions = [...data.stateRevisions]
  snapshotCache.set(sessionId, existing)
}

function getSessionSnapshot(sessionId: number): SessionSnapshot | undefined {
  return snapshotCache.get(sessionId)
}

// ---------------------------------------------------------------------------
// Session list state
// ---------------------------------------------------------------------------
const sessions = ref<AgentSessionMeta[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const staleSessionIds = ref<Set<number>>(new Set())

async function refresh() {
  loading.value = true
  error.value = null
  try {
    const fresh = await apiGet<AgentSessionMeta[]>('/sessions')
    // Merge: fresh sessions first, then stale sessions that aren't in the fresh list
    const freshIds = new Set(fresh.map((s) => s.sessionId))
    const kept = sessions.value.filter(
      (s) => staleSessionIds.value.has(s.sessionId) && !freshIds.has(s.sessionId),
    )
    sessions.value = [...fresh, ...kept]
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load sessions'
  } finally {
    loading.value = false
  }
}

async function createSession(agentName: string, title: string): Promise<AgentSessionMeta> {
  const body: CreateSessionRequest = { agentName, title }
  const meta = await apiPost<AgentSessionMeta>('/sessions', body)
  await refresh() // Refresh list so the new session appears
  return meta
}

/** Mark all current sessions as stale (backend was restarted). */
function markAllStale() {
  for (const s of sessions.value) {
    staleSessionIds.value.add(s.sessionId)
  }
  // Replace the Set to trigger Vue reactivity
  staleSessionIds.value = new Set(staleSessionIds.value)
}

/** Check whether a session is stale (existed before a backend restart). */
function isSessionStale(id: number): boolean {
  return staleSessionIds.value.has(id)
}

// Auto-fetch on module load
refresh()

export function useSessions() {
  return {
    sessions,
    loading,
    error,
    refresh,
    createSession,
    staleSessionIds,
    markAllStale,
    isSessionStale,
    cacheSessionData,
    getSessionSnapshot,
  }
}
