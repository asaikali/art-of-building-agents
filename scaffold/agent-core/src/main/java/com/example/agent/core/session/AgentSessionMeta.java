package com.example.agent.core.session;

import java.time.Instant;

public record AgentSessionMeta(
    SessionId sessionId,
    String title,
    String agentName,
    String userId,
    long stateRev,
    long eventCount,
    Instant lastUpdatedAt) {}
