<script setup lang="ts">
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import { useChat } from '@/composables/useChat'

const md = new MarkdownIt({ html: false })

const props = defineProps<{
  sessionId: string
  isStale?: boolean
}>()

const sessionIdRef = computed(() => props.sessionId || null)
const isStaleRef = computed(() => props.isStale ?? false)
const { messages, loading, waiting, error, sendMessage, refresh } = useChat(sessionIdRef, isStaleRef)

const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

watch(inputText, () => nextTick(autoResize))

// Elapsed timer for waiting indicator
const elapsedSeconds = ref(0)
let timerInterval: ReturnType<typeof setInterval> | null = null

watch(waiting, (isWaiting) => {
  if (isWaiting) {
    elapsedSeconds.value = 0
    timerInterval = setInterval(() => {
      elapsedSeconds.value++
    }, 1000)
  } else {
    if (timerInterval) {
      clearInterval(timerInterval)
      timerInterval = null
    }
  }
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
})

// Animated dots: cycle 1-5 dots based on elapsed time
const animatedDots = computed(() => {
  const count = (elapsedSeconds.value % 5) + 1
  return '.'.repeat(count)
})

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(messages, scrollToBottom)
watch(waiting, scrollToBottom)

function renderMarkdown(text: string): string {
  return DOMPurify.sanitize(md.render(text))
}

function formatTime(ts: string): string {
  return new Date(ts).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })
}

async function handleSend() {
  if (!inputText.value.trim() || props.isStale) return
  const text = inputText.value.trim()
  inputText.value = ''
  await sendMessage(text)
}

function handleKeydown(e: KeyboardEvent) {
  if (props.isStale) return
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

// Expose refresh so parent can trigger it from event signals
defineExpose({ refreshMessages: refresh })
</script>

<template>
  <div class="flex flex-col h-full bg-white">
    <!-- Panel title bar -->
    <div class="flex items-center justify-between bg-gray-100 border-b border-gray-300 px-4 py-2.5 shrink-0">
      <span class="text-base font-bold text-gray-800">Chat</span>
      <span class="text-sm font-semibold text-gray-700 font-mono">{{ messages.length }} messages</span>
    </div>

    <!-- Stale session banner -->
    <div v-if="props.isStale" class="bg-amber-50 border-b border-amber-200 px-4 py-2 text-sm text-amber-800 flex items-center gap-2 shrink-0">
      <span class="text-amber-500 shrink-0">&#9888;</span>
      <span>Backend was restarted — this session is read-only. Create a new session to continue.</span>
    </div>

    <!-- Messages -->
    <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-4">
      <!-- Loading state -->
      <p v-if="loading && messages.length === 0" class="text-sm text-gray-400 text-center py-8">Loading messages...</p>

      <!-- Empty state -->
      <p v-else-if="messages.length === 0" class="text-sm text-gray-400 text-center py-8">No messages yet. Start the conversation.</p>

      <!-- Error state -->
      <div v-if="error && !props.isStale" class="text-sm text-red-500 text-center py-2">
        {{ error }}
        <button @click="refresh" class="ml-2 underline hover:no-underline">Retry</button>
      </div>

      <div
        v-for="(msg, i) in messages"
        :key="i"
        class="flex"
        :class="msg.role === 'USER' ? 'justify-end' : 'justify-start'"
      >
        <div
          class="max-w-[80%] rounded-lg px-4 py-2 text-sm"
          :class="
            msg.role === 'USER'
              ? 'bg-blue-500 text-white'
              : 'bg-gray-100 text-gray-900'
          "
        >
          <p v-if="msg.role === 'USER'" class="whitespace-pre-wrap">{{ msg.text }}</p>
          <div v-else class="prose prose-sm max-w-none" v-html="renderMarkdown(msg.text)" />
          <p
            class="text-xs mt-1"
            :class="msg.role === 'USER' ? 'text-blue-200' : 'text-gray-400'"
          >
            {{ formatTime(msg.ts) }}
          </p>
        </div>
      </div>

      <!-- Waiting for response indicator -->
      <div v-if="waiting" class="flex justify-start">
        <div class="bg-gray-100 text-gray-500 rounded-lg px-4 py-2 text-sm flex items-center gap-3">
          <span class="font-mono w-12">{{ animatedDots }}</span>
          <span class="text-xs text-gray-400">{{ elapsedSeconds }}s</span>
        </div>
      </div>
    </div>

    <!-- Input -->
    <div class="border-t border-gray-200 p-3">
      <div class="flex gap-2">
        <textarea
          ref="textareaRef"
          v-model="inputText"
          @keydown="handleKeydown"
          rows="2"
          :placeholder="props.isStale ? 'Session is read-only' : 'Type a message... (Shift+Enter for new line)'"
          :disabled="props.isStale"
          class="flex-1 resize-y rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent min-h-[2.5rem] max-h-[25rem] overflow-y-auto disabled:bg-gray-100 disabled:cursor-not-allowed"
        />
        <button
          @click="handleSend"
          class="px-4 py-2 bg-blue-500 text-white rounded-md text-sm font-medium hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="!inputText.trim() || waiting || props.isStale"
        >
          Send
        </button>
      </div>
    </div>
  </div>
</template>
