// Composable for managing chat messages within a session.
// Fetches messages via REST, sends new ones, and supports
// event-driven refresh from the parent component.
// For stale sessions, restores messages from the snapshot cache
// instead of hitting the (now-dead) backend.

import { ref, watch, type Ref, type ComputedRef } from 'vue'
import { apiGet, apiPost } from '@/composables/useApi'
import { useSessions } from '@/composables/useSessions'
import type { AgentMessage, AppendMessageRequest } from '@/types'

export function useChat(
  sessionId: Ref<string | null> | ComputedRef<string | null>,
  isStale?: Ref<boolean> | ComputedRef<boolean>,
) {
  const messages = ref<AgentMessage[]>([])
  const loading = ref(false)
  const waiting = ref(false)
  const error = ref<string | null>(null)
  const { cacheSessionData, getSessionSnapshot, isSessionStale } = useSessions()

  async function refresh() {
    const id = sessionId.value
    if (!id) return

    loading.value = true
    error.value = null
    try {
      messages.value = await apiGet<AgentMessage[]>(`/sessions/${id}/messages`)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load messages'
    } finally {
      loading.value = false
    }
  }

  async function sendMessage(text: string) {
    const id = sessionId.value
    if (!id || !text.trim()) return

    // Optimistically add the user message
    const optimistic: AgentMessage = {
      ts: new Date().toISOString(),
      role: 'USER',
      text: text.trim(),
    }
    messages.value.push(optimistic)
    error.value = null
    waiting.value = true

    try {
      const body: AppendMessageRequest = { role: 'USER', text: text.trim() }
      await apiPost<AgentMessage>(`/sessions/${id}/messages`, body)

      // Refresh after a short delay to pick up any agent response
      setTimeout(async () => {
        await refresh()
        waiting.value = false
      }, 500)
    } catch (e) {
      // Remove the optimistic message on failure
      messages.value = messages.value.filter((m) => m !== optimistic)
      error.value = e instanceof Error ? e.message : 'Failed to send message'
      waiting.value = false
    }
  }

  // Re-fetch messages when sessionId changes
  watch(
    sessionId,
    (newId) => {
      messages.value = []
      if (newId) {
        const numId = Number(newId)
        // For stale sessions, restore from cache instead of fetching from backend
        if (isSessionStale(numId)) {
          const snapshot = getSessionSnapshot(numId)
          if (snapshot) {
            messages.value = [...snapshot.messages]
          }
        } else {
          refresh()
        }
      }
    },
    { immediate: true },
  )

  // When a session transitions to stale, snapshot current messages into cache
  if (isStale) {
    watch(isStale, (stale) => {
      const id = sessionId.value
      if (stale && id && messages.value.length > 0) {
        cacheSessionData(Number(id), { messages: messages.value })
      }
    })
  }

  return { messages, loading, waiting, error, sendMessage, refresh }
}
