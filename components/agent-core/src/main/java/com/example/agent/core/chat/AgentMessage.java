package com.example.agent.core.chat;

import java.time.Instant;

public record AgentMessage(Instant ts, Role role, String text) {}
