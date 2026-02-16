<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useSessions } from '@/composables/useSessions'
import { useHeartbeat } from '@/composables/useHeartbeat'

const props = defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  toggle: []
}>()

const router = useRouter()
const route = useRoute()

const { sessions, loading, createSession, isSessionStale } = useSessions()
const { agentName: heartbeatAgentName, status: heartbeatStatus } = useHeartbeat()

const currentSessionId = computed(() => Number(route.params.id))

function navigateTo(id: number) {
  router.push(`/sessions/${id}`)
}

async function handleNewChat() {
  const title = window.prompt('Session name:')
  if (title === null) return // user cancelled

  try {
    const agentName = heartbeatAgentName.value ?? 'Agent'
    const meta = await createSession(agentName, title.trim() || 'New Session')
    router.push(`/sessions/${meta.sessionId}`)
  } catch (e) {
    console.error('Failed to create session:', e)
  }
}
</script>

<template>
  <nav class="h-full bg-gray-100 border-r border-gray-200 flex flex-col overflow-hidden">
    <!-- Top bar: toggle only -->
    <div class="flex items-center p-2" :class="{ 'justify-center': props.collapsed }">
      <button
        @click="emit('toggle')"
        class="p-1.5 rounded hover:bg-gray-200 text-gray-500 shrink-0"
        :title="props.collapsed ? 'Expand sidebar' : 'Collapse sidebar'"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd" />
        </svg>
      </button>
    </div>

    <!-- Session list -->
    <div v-show="!props.collapsed" class="flex-1 overflow-y-auto px-2">
      <!-- Loading state -->
      <p v-if="loading" class="text-sm text-gray-400 text-center py-4">Loading...</p>

      <!-- Empty state -->
      <p v-else-if="sessions.length === 0" class="text-sm text-gray-400 text-center py-4">No sessions yet</p>

      <!-- Session list -->
      <ul v-else class="space-y-1">
        <li v-for="session in sessions" :key="session.sessionId">
          <button
            @click="navigateTo(session.sessionId)"
            class="w-full text-left px-3 py-2 rounded-md text-sm truncate transition-colors"
            :class="[
              currentSessionId === session.sessionId
                ? 'bg-gray-900 text-white'
                : isSessionStale(session.sessionId)
                  ? 'text-gray-400 hover:bg-gray-100'
                  : 'text-gray-700 hover:bg-gray-200',
            ]"
          >
            <span v-if="isSessionStale(session.sessionId)" class="text-amber-500 mr-1">&#9679;</span>
            <span class="text-xs opacity-60">{{ session.agentName }}</span>
            <span class="opacity-40 mx-0.5">&mdash;</span>
            {{ session.title }}
          </button>
        </li>
      </ul>
    </div>

    <!-- New Chat button at bottom -->
    <div v-show="!props.collapsed" class="p-2 shrink-0">
      <button
        @click="handleNewChat"
        :disabled="heartbeatStatus !== 'connected'"
        class="w-full text-sm font-semibold text-white bg-blue-600 rounded-md px-3 py-2 hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        + New Chat
      </button>
    </div>
  </nav>
</template>
