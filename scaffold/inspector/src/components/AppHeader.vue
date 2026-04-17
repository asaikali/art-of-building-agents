<script setup lang="ts">
defineProps<{
  agentName?: string
  sessionTitle?: string
  sessionId?: string
  sseStatus?: 'connected' | 'connecting' | 'disconnected'
  isStale?: boolean
}>()
</script>

<template>
  <header class="sticky top-0 z-50 bg-gray-900 text-white px-4 py-2 flex justify-between items-center text-sm">
    <!-- Left side: agent + session info -->
    <div class="flex items-center gap-3">
      <span class="font-semibold text-white">{{ agentName ?? 'Agent' }}</span>
      <span class="text-gray-400">/</span>
      <span class="text-gray-300">{{ sessionTitle ?? 'Untitled Session' }}</span>
      <span v-if="isStale" class="text-xs bg-amber-500 text-white px-1.5 py-0.5 rounded-full">read-only</span>
      <span class="text-gray-500 text-xs font-mono">#{{ sessionId ?? '–' }}</span>
    </div>

    <!-- Right side: SSE connection status -->
    <div class="flex items-center gap-1.5">
      <span
        class="inline-block w-2 h-2 rounded-full"
        :class="{
          'bg-green-400': sseStatus === 'connected',
          'bg-yellow-400': sseStatus === 'connecting',
          'bg-red-400': sseStatus === 'disconnected' || !sseStatus,
        }"
      />
      <span class="text-gray-400">{{ sseStatus ?? 'disconnected' }}</span>
    </div>
  </header>
</template>
