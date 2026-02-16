package com.example.agent.core.event;

import java.time.Instant;
import java.util.Map;

public record AgentEvent(Instant ts, long seq, String msg, Map<String, Object> data) {}
