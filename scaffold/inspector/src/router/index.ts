import { createRouter, createWebHistory } from 'vue-router'
import InspectorView from '@/views/InspectorView.vue'
import ChatPopout from '@/views/ChatPopout.vue'
import StatePopout from '@/views/StatePopout.vue'
import EventsPopout from '@/views/EventsPopout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: InspectorView,
    },
    {
      path: '/sessions',
      name: 'sessions',
      component: InspectorView,
    },
    {
      path: '/sessions/:id',
      name: 'inspector',
      component: InspectorView,
    },
    {
      path: '/sessions/:id/chat',
      name: 'chat-popout',
      component: ChatPopout,
    },
    {
      path: '/sessions/:id/state',
      name: 'state-popout',
      component: StatePopout,
    },
    {
      path: '/sessions/:id/events',
      name: 'events-popout',
      component: EventsPopout,
    },
  ],
})

export default router
