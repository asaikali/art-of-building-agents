// Generic SSE composable with automatic reconnection.
// Works with both events and state streams — the backend replays
// all history on connect, so reconnect simply clears and refills.

import { ref, watch, onUnmounted, type Ref, type ComputedRef } from 'vue'
import type { SseStatus } from '@/types'

export function useSse<T>(urlRef: Ref<string | null> | ComputedRef<string | null>) {
  const items = ref<T[]>([]) as Ref<T[]>
  const status = ref<SseStatus>('disconnected')

  let eventSource: EventSource | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let reconnectDelay = 1000
  const MAX_RECONNECT_DELAY = 30000
  const HEARTBEAT_INTERVAL = 5000 // check every 5 seconds

  function close() {
    if (heartbeatTimer !== null) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
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

  function connect(url: string) {
    close()
    items.value = []
    status.value = 'connecting'
    reconnectDelay = 1000

    const es = new EventSource(url)
    eventSource = es

    es.onopen = () => {
      status.value = 'connected'
      reconnectDelay = 1000 // Reset backoff on successful connection

      // Poll readyState to detect silent disconnects
      if (heartbeatTimer) clearInterval(heartbeatTimer)
      heartbeatTimer = setInterval(() => {
        if (es.readyState === EventSource.CLOSED) {
          es.dispatchEvent(new Event('error'))
        }
      }, HEARTBEAT_INTERVAL)
    }

    es.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data) as T
        items.value.push(parsed)
      } catch {
        console.warn('[useSse] Failed to parse SSE data:', event.data)
      }
    }

    es.onerror = () => {
      // Close the native EventSource to prevent its automatic reconnection
      es.close()
      eventSource = null
      status.value = 'disconnected'

      // Schedule manual reconnect with exponential backoff
      reconnectTimer = setTimeout(() => {
        reconnectTimer = null
        const currentUrl = urlRef.value
        if (currentUrl) {
          connect(currentUrl)
        }
      }, reconnectDelay)

      reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY)
    }
  }

  // React to URL changes (e.g., session switch)
  watch(
    urlRef,
    (newUrl) => {
      if (newUrl) {
        connect(newUrl)
      } else {
        close()
        items.value = []
        status.value = 'disconnected'
      }
    },
    { immediate: true },
  )

  // Clean up on component unmount
  onUnmounted(() => {
    close()
  })

  return { items, status, close }
}
