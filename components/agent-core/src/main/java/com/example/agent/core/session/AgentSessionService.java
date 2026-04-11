package com.example.agent.core.session;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.Role;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AgentSessionService {

  private final SessionManager sessionManager;
  private final AgentHandler agentHandler;

  public AgentSessionService(SessionManager sessionManager, AgentHandler agentHandler) {
    this.sessionManager = sessionManager;
    this.agentHandler = agentHandler;
  }

  public Session createSession(String title) {
    Session session = sessionManager.createSession(agentHandler.getName(), title);
    session.logEvent("session-started", Map.of("agent", agentHandler.getName()));

    String initialMessage = agentHandler.getInitialAssistantMessage();
    if (initialMessage != null && !initialMessage.isBlank()) {
      session.appendMessage(Role.ASSISTANT, initialMessage.trim());
    }

    return session;
  }
}
