package com.example.agent.core.session;

public class SessionNotFoundException extends RuntimeException {
  public SessionNotFoundException(SessionId sessionId) {
    super("Session not found: " + sessionId.value());
  }
}
