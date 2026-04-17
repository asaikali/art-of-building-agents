// Global heartbeat SSE composable — singleton, not tied to any component lifecycle.
// Provides the agent name and backend connection status for the header.
// Uses a staleness check: if no heartbeat arrives within STALE_THRESHOLD_MS,
// the connection is considered dead and a reconnect is triggered.

import { ref, computed } from 'vue'
import type { SseStatus, HeartbeatMessage } from '@/types'

const status = ref<SseStatus>('disconnected')
const latestAgentName = ref<string | null>(null)

let eventSource: EventSource | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let stalenessTimer: ReturnType<typeof setInterval> | null = null
let lastHeartbeatAt = 0
let reconnectDelay = 1000
const MAX_RECONNECT_DELAY = 30000
const HEARTBEAT_URL = '/api/heartbeat/stream'
const STALE_THRESHOLD_MS = 5000 // no heartbeat for 5s = dead
const STALE_CHECK_INTERVAL_MS = 2000 // check every 2s

function close() {
  if (stalenessTimer !== null) {
    clearInterval(stalenessTimer)
    stalenessTimer = null
  }
  if (reconnectTimer !== null) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

function scheduleReconnect() {
  status.value = 'disconnected'
  latestAgentName.value = null

  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connect()
  }, reconnectDelay)

  reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY)
}

function connect() {
  close()
  status.value = 'connecting'
  reconnectDelay = 1000
  lastHeartbeatAt = 0

  const es = new EventSource(HEARTBEAT_URL)
  eventSource = es

  es.onopen = () => {
    status.value = 'connected'
    reconnectDelay = 1000
    lastHeartbeatAt = Date.now()

    // Start staleness checker — detects silent disconnects
    if (stalenessTimer) clearInterval(stalenessTimer)
    stalenessTimer = setInterval(() => {
      if (lastHeartbeatAt > 0 && Date.now() - lastHeartbeatAt > STALE_THRESHOLD_MS) {
        // No heartbeat received recently — force reconnect
        close()
        scheduleReconnect()
      }
    }, STALE_CHECK_INTERVAL_MS)
  }

  es.onmessage = (event) => {
    lastHeartbeatAt = Date.now()
    try {
      const parsed = JSON.parse(event.data) as HeartbeatMessage
      latestAgentName.value = parsed.agentName
    } catch {
      console.warn('[useHeartbeat] Failed to parse heartbeat:', event.data)
    }
  }

  es.onerror = () => {
    es.close()
    eventSource = null
    close()
    scheduleReconnect()
  }
}

// Auto-connect on module load
connect()

export function useHeartbeat() {
  return {
    /** SSE connection status: 'connected' | 'connecting' | 'disconnected' */
    status,
    /** The agent name from the latest heartbeat, null when disconnected */
    agentName: computed(() => latestAgentName.value),
  }
}
