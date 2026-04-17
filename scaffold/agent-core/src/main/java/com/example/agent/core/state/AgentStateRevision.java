package com.example.agent.core.state;

import com.example.agent.core.session.SessionId;
import java.time.Instant;

public record AgentStateRevision(Instant ts, long rev, SessionId sessionId, String markdown) {}
