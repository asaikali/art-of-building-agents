<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import ChatPanel from '@/components/ChatPanel.vue'
import { apiGet } from '@/composables/useApi'
import { useSessions } from '@/composables/useSessions'
import type { AgentSessionMeta } from '@/types'

const route = useRoute()
const sessionId = computed(() => route.params.id as string)
const meta = ref<AgentSessionMeta | null>(null)
const { isSessionStale } = useSessions()

const isStale = computed(() => {
  const id = Number(sessionId.value)
  return id ? isSessionStale(id) : false
})

watch(
  sessionId,
  async (id) => {
    if (id) {
      try {
        meta.value = await apiGet<AgentSessionMeta>(`/sessions/${id}/meta`)
      } catch {
        if (!isStale.value) {
          meta.value = null
        }
      }
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="h-screen flex flex-col">
    <AppHeader
      :agent-name="meta?.agentName ?? 'Agent'"
      :session-title="meta?.title ?? 'Loading...'"
      :session-id="sessionId"
      sse-status="disconnected"
      :is-stale="isStale"
    />
    <ChatPanel :session-id="sessionId" :is-stale="isStale" class="flex-1" />
  </div>
</template>
