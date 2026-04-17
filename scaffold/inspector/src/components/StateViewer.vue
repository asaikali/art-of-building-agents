<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import { useSse } from '@/composables/useSse'
import { useSessions } from '@/composables/useSessions'
import type { AgentStateRevision } from '@/types'

const props = defineProps<{
  sessionId: string
  isStale?: boolean
}>()

const md = new MarkdownIt({ html: false })
const { cacheSessionData, getSessionSnapshot, isSessionStale } = useSessions()

// SSE connection for live state revisions (disabled for stale sessions to avoid reconnect loop)
const sseUrl = computed(() =>
  props.sessionId && !props.isStale ? `/api/sessions/${props.sessionId}/state/stream` : null,
)
const { items: sseRevisions } = useSse<AgentStateRevision>(sseUrl)

// Displayed revisions — either live SSE data or cached snapshot for stale sessions
const revisions = ref<AgentStateRevision[]>([])

// Sync live SSE revisions into the display ref
watch(sseRevisions, (items) => {
  revisions.value = items
}, { immediate: true })

// When session transitions to stale, snapshot state revisions into cache before SSE clears them
watch(
  () => props.isStale,
  (stale) => {
    if (stale && props.sessionId && revisions.value.length > 0) {
      cacheSessionData(Number(props.sessionId), { stateRevisions: revisions.value })
    }
  },
)

// When navigating to a stale session, restore revisions from cache
watch(
  () => props.sessionId,
  (newId) => {
    if (newId) {
      const numId = Number(newId)
      if (isSessionStale(numId)) {
        const snapshot = getSessionSnapshot(numId)
        if (snapshot) {
          revisions.value = [...snapshot.stateRevisions]
        }
      }
    }
  },
)

// Navigation state
const currentRevIndex = ref(-1)

// Auto-advance to latest revision when a new one arrives
watch(
  () => revisions.value.length,
  (newLen, oldLen) => {
    if (newLen === 0) {
      currentRevIndex.value = -1
    } else if (currentRevIndex.value === (oldLen ?? 0) - 1 || (oldLen ?? 0) === 0) {
      // Auto-advance if user was viewing the latest (or first time loading)
      currentRevIndex.value = newLen - 1
    }
  },
)

const currentRevision = computed(() =>
  currentRevIndex.value >= 0 && currentRevIndex.value < revisions.value.length
    ? revisions.value[currentRevIndex.value]
    : null,
)

const renderedHtml = computed(() =>
  currentRevision.value
    ? DOMPurify.sanitize(md.render(currentRevision.value.markdown))
    : '',
)

function formatTime(ts: string): string {
  return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function prevRev() {
  if (currentRevIndex.value > 0) currentRevIndex.value--
}
function nextRev() {
  if (currentRevIndex.value < revisions.value.length - 1) currentRevIndex.value++
}
</script>

<template>
  <div class="flex flex-col h-full bg-white">
    <!-- Panel title bar -->
    <div class="flex items-center justify-between bg-gray-100 border-b border-gray-300 px-4 py-2.5 shrink-0">
      <!-- Left: nav arrows + title + timestamp + rev -->
      <div class="flex items-center">
        <!-- Only show navigation when there are revisions -->
        <template v-if="currentRevision">
          <!-- left arrow -->
          <button
            @click="prevRev"
            :disabled="currentRevIndex === 0"
            class="px-1.5 py-0.5 rounded hover:bg-gray-200 disabled:opacity-30 disabled:cursor-not-allowed text-sm font-semibold text-gray-700 mr-2"
          >
            &larr;
          </button>

          <!-- Title + timestamp + rev counter -->
          <span class="text-base font-bold text-gray-800">State</span>
          <span class="text-base font-bold text-gray-800 mx-1">@</span>
          <span class="text-base font-bold text-gray-800 font-mono">
            {{ formatTime(currentRevision.ts) }}
          </span>
          <span class="text-base font-bold text-gray-800 font-mono ml-2">
            (rev {{ currentRevision.rev }})
          </span>

          <!-- right arrow -->
          <button
            @click="nextRev"
            :disabled="currentRevIndex === revisions.length - 1"
            class="px-1.5 py-0.5 rounded hover:bg-gray-200 disabled:opacity-30 disabled:cursor-not-allowed text-sm font-semibold text-gray-700 ml-2"
          >
            &rarr;
          </button>
        </template>

        <!-- No revisions yet -->
        <span v-else class="text-base font-bold text-gray-800">State</span>
      </div>

      <!-- Right: revision count -->
      <span class="text-sm font-semibold text-gray-700 font-mono">{{ revisions.length }} revisions</span>
    </div>

    <!-- Rendered markdown -->
    <div class="flex-1 overflow-y-auto p-4">
      <!-- Empty state -->
      <p v-if="!currentRevision" class="text-sm text-gray-400 text-center py-8">No state revisions yet</p>

      <div v-else class="prose prose-sm max-w-none" v-html="renderedHtml" />
    </div>
  </div>
</template>
