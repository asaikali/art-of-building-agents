<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Splitpanes, Pane } from 'splitpanes'
import AppHeader from '@/components/AppHeader.vue'
import SessionNavigator from '@/components/SessionNavigator.vue'
import ChatPanel from '@/components/ChatPanel.vue'
import StateViewer from '@/components/StateViewer.vue'
import EventsViewer from '@/components/EventsViewer.vue'
import { apiGet } from '@/composables/useApi'
import { useHeartbeat } from '@/composables/useHeartbeat'
import { useSessions } from '@/composables/useSessions'
import type { AgentSessionMeta } from '@/types'

const route = useRoute()
const { status: heartbeatStatus, agentName: heartbeatAgentName } = useHeartbeat()
const { refresh: refreshSessions, markAllStale, isSessionStale } = useSessions()

// When the backend reconnects, mark old sessions as stale (read-only)
let wasDisconnected = false
watch(heartbeatStatus, (newStatus) => {
  if (newStatus === 'disconnected' || newStatus === 'connecting') {
    wasDisconnected = true
  } else if (newStatus === 'connected' && wasDisconnected) {
    wasDisconnected = false
    markAllStale()
    refreshSessions()
    // Stay on current session — it becomes read-only
  }
})

// Reactive sessionId — updates when the user navigates to a different session
const sessionId = computed(() => (route.params.id as string) ?? '')

// Whether the current session is stale (backend was restarted since it was created)
const isStale = computed(() => {
  const id = Number(sessionId.value)
  return id ? isSessionStale(id) : false
})

// Fetch session metadata for the header
const meta = ref<AgentSessionMeta | null>(null)

watch(
  sessionId,
  async (id) => {
    if (id) {
      try {
        meta.value = await apiGet<AgentSessionMeta>(`/sessions/${id}/meta`)
      } catch {
        // If stale, keep the last-known meta so the header still shows the title
        if (!isStale.value) {
          meta.value = null
        }
      }
    } else {
      meta.value = null
    }
  },
  { immediate: true },
)

// Template refs for child components
const chatPanelRef = ref<InstanceType<typeof ChatPanel> | null>(null)

// Navigator collapse state — controlled here, passed to child as prop
const navCollapsed = ref(false)
const NAV_EXPANDED_SIZE = 18
const NAV_COLLAPSED_SIZE = 2.5
const NAV_AUTO_COLLAPSE_THRESHOLD = 5

// All three pane sizes must be reactive and sum to 100%
const navigatorSize = ref(NAV_EXPANDED_SIZE)
const chatSize = ref(35)
const rightSize = ref(100 - NAV_EXPANDED_SIZE - 35) // 47

function toggleNav() {
  navCollapsed.value = !navCollapsed.value
  const newNavSize = navCollapsed.value ? NAV_COLLAPSED_SIZE : NAV_EXPANDED_SIZE
  const freed = navigatorSize.value - newNavSize
  // Distribute freed space proportionally to chat and right panes
  const total = chatSize.value + rightSize.value
  chatSize.value += freed * (chatSize.value / total)
  rightSize.value += freed * (rightSize.value / total)
  navigatorSize.value = newNavSize
}

function onResize(panes: { size: number }[]) {
  if (panes.length >= 3) {
    navigatorSize.value = panes[0].size
    chatSize.value = panes[1].size
    rightSize.value = panes[2].size
    // Auto-collapse when dragged below threshold
    navCollapsed.value = panes[0].size < NAV_AUTO_COLLAPSE_THRESHOLD
  }
}
</script>

<template>
  <div class="h-screen flex flex-col">
    <AppHeader
      :agent-name="heartbeatAgentName ?? 'Agent'"
      :session-title="meta?.title ?? (sessionId ? 'Loading...' : 'Select a session')"
      :session-id="sessionId || undefined"
      :sse-status="heartbeatStatus"
      :is-stale="isStale"
    />

    <Splitpanes class="default-theme flex-1" @resize="onResize">
      <!-- Session navigator -->
      <Pane :size="navigatorSize" :min-size="3">
        <SessionNavigator :collapsed="navCollapsed" @toggle="toggleNav" />
      </Pane>

      <!-- Chat panel -->
      <Pane :size="chatSize" :min-size="15">
        <ChatPanel v-if="sessionId" ref="chatPanelRef" :session-id="sessionId" :is-stale="isStale" />
        <div v-else class="flex items-center justify-center h-full bg-white text-gray-400 text-sm">
          Select a session to start chatting
        </div>
      </Pane>

      <!-- Right side: State + Events stacked vertically -->
      <Pane :size="rightSize" :min-size="20">
        <Splitpanes horizontal class="default-theme">
          <Pane :size="50" :min-size="20">
            <StateViewer v-if="sessionId" :session-id="sessionId" :is-stale="isStale" />
            <div v-else class="flex items-center justify-center h-full bg-white text-gray-400 text-sm">
              Select a session
            </div>
          </Pane>
          <Pane :size="50" :min-size="20">
            <EventsViewer v-if="sessionId" :session-id="sessionId" :is-stale="isStale" />
            <div v-else class="flex items-center justify-center h-full bg-white text-gray-400 text-sm">
              Select a session
            </div>
          </Pane>
        </Splitpanes>
      </Pane>
    </Splitpanes>
  </div>
</template>
