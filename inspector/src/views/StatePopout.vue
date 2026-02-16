<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import StateViewer from '@/components/StateViewer.vue'
import { apiGet } from '@/composables/useApi'
import type { AgentSessionMeta } from '@/types'

const route = useRoute()
const sessionId = computed(() => route.params.id as string)
const meta = ref<AgentSessionMeta | null>(null)

watch(
  sessionId,
  async (id) => {
    if (id) {
      try {
        meta.value = await apiGet<AgentSessionMeta>(`/sessions/${id}/meta`)
      } catch {
        meta.value = null
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
    />
    <StateViewer :session-id="sessionId" class="flex-1" />
  </div>
</template>
