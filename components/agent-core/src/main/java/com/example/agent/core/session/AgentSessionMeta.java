package com.example.agent.core.session;

import java.time.Instant;

public record AgentSessionMeta(
    SessionId sessionId,
    String title,
    String agentName,
    long stateRev,
    long eventCount,
    Instant lastUpdatedAt) {}
