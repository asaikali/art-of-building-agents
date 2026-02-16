<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useSse } from '@/composables/useSse'
import { useSessions } from '@/composables/useSessions'
import type { AgentEvent } from '@/types'

const props = defineProps<{
  sessionId: string
  isStale?: boolean
}>()

const { cacheSessionData, getSessionSnapshot, isSessionStale } = useSessions()

// SSE connection for live events (disabled for stale sessions to avoid reconnect loop)
const sseUrl = computed(() =>
  props.sessionId && !props.isStale ? `/api/sessions/${props.sessionId}/events/stream` : null,
)
const { items: sseEvents } = useSse<AgentEvent>(sseUrl)

// Displayed events — either live SSE data or cached snapshot for stale sessions
const events = ref<AgentEvent[]>([])

// Sync live SSE events into the display ref
watch(sseEvents, (items) => {
  events.value = items
}, { immediate: true })

// When session transitions to stale, snapshot events into cache before SSE clears them
watch(
  () => props.isStale,
  (stale) => {
    if (stale && props.sessionId && events.value.length > 0) {
      cacheSessionData(Number(props.sessionId), { events: events.value })
    }
  },
)

// When navigating to a stale session, restore events from cache
watch(
  () => props.sessionId,
  (newId) => {
    if (newId) {
      const numId = Number(newId)
      if (isSessionStale(numId)) {
        const snapshot = getSessionSnapshot(numId)
        if (snapshot) {
          events.value = [...snapshot.events]
        }
      }
    }
  },
)

// Track expanded state separately (UI-only, keyed by event.seq)
const expandedSeqs = ref(new Set<number>())

function formatTime(ts: string): string {
  return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function toggleExpand(seq: number) {
  if (expandedSeqs.value.has(seq)) {
    expandedSeqs.value.delete(seq)
  } else {
    expandedSeqs.value.add(seq)
  }
}

function isExpanded(seq: number): boolean {
  return expandedSeqs.value.has(seq)
}

</script>

<template>
  <div class="flex flex-col h-full bg-white">
    <!-- Panel title bar -->
    <div class="flex items-center justify-between bg-gray-100 border-b border-gray-300 px-4 py-2.5 shrink-0">
      <span class="text-base font-bold text-gray-800">Events</span>
      <span class="text-sm font-semibold text-gray-700 font-mono">{{ events.length }} events</span>
    </div>

    <!-- Event list -->
    <div class="flex-1 overflow-y-auto">
      <!-- Empty state -->
      <p v-if="events.length === 0" class="text-sm text-gray-400 text-center py-8">Waiting for events...</p>

      <div
        v-for="event in events"
        :key="event.seq"
        class="border-b border-gray-100 text-sm"
      >
        <!-- Event row -->
        <div
          class="flex items-center gap-0 px-3 py-2 hover:bg-gray-50 cursor-pointer"
          @click="event.data && toggleExpand(event.seq)"
        >
          <!-- Expand chevron -->
          <span
            v-if="event.data"
            class="text-gray-400 transition-transform text-xs select-none w-5 shrink-0 text-center"
            :class="{ 'rotate-90': isExpanded(event.seq) }"
          >
            &#9654;
          </span>
          <span v-else class="w-5 shrink-0" />

          <!-- Sequence number badge -->
          <span class="inline-flex items-center justify-center bg-gray-200 text-gray-600 font-mono text-xs font-semibold rounded px-1.5 py-0.5 min-w-[1.5rem] shrink-0">
            {{ event.seq }}
          </span>

          <!-- Separator -->
          <span class="mx-2 text-gray-300 shrink-0">&vert;</span>

          <!-- Time -->
          <span class="text-gray-600 font-mono text-sm shrink-0">
            {{ formatTime(event.ts) }}
          </span>

          <!-- Separator -->
          <span class="mx-2 text-gray-300 shrink-0">&vert;</span>

          <!-- Message -->
          <span class="text-gray-700 text-sm break-all">{{ event.msg }}</span>
        </div>

        <!-- Expanded JSON data -->
        <div v-if="isExpanded(event.seq) && event.data" class="px-3 pb-3 pl-14">
          <pre class="text-xs bg-gray-50 rounded p-3 overflow-x-auto text-gray-600 border border-gray-200">{{ JSON.stringify(event.data, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>
