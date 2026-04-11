// TypeScript interfaces mirroring the backend Java records.
// Property names match the JSON that Spring Boot serializes.

// --- Enums ---

export type Role = 'USER' | 'ASSISTANT'

export type SseStatus = 'connected' | 'connecting' | 'disconnected'

// --- REST response types ---

export interface AgentSessionMeta {
  sessionId: number
  title: string
  agentName: string
  stateRev: number
  eventCount: number
  lastUpdatedAt: string // Instant serializes as ISO-8601 string
}

export interface AgentMessage {
  ts: string // Instant → ISO-8601 string
  role: Role
  text: string
}

export interface AgentEvent {
  ts: string
  seq: number
  msg: string
  data: unknown // JsonNode → arbitrary JSON
}

export interface AgentStateRevision {
  ts: string
  rev: number
  sessionId: number
  markdown: string
}

export interface HeartbeatMessage {
  agentName: string
  ts: string // Instant → ISO-8601 string
}

// --- Request body types ---

export interface CreateSessionRequest {
  title: string
}

export interface AppendMessageRequest {
  role: Role
  text: string
}
