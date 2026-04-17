package com.example.agent.core.state;

import com.example.agent.core.session.SessionId;

public class RevisionNotFoundException extends RuntimeException {
  public RevisionNotFoundException(SessionId sessionId, long rev) {
    super("Revision " + rev + " not found for session " + sessionId.value());
  }
}
