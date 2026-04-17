package com.example.agent.core.heartbeat;

import java.time.Instant;

public record HeartbeatMessage(String agentName, Instant ts) {}
