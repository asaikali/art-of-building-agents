package com.example.agent.core.session;

import com.example.agent.core.event.EventService;
import com.example.agent.core.state.StateService;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SessionMetaAssembler {

  private final SessionManager sessionManager;
  private final EventService eventService;
  private final StateService stateService;

  public SessionMetaAssembler(
      SessionManager sessionManager, EventService eventService, StateService stateService) {
    this.sessionManager = sessionManager;
    this.eventService = eventService;
    this.stateService = stateService;
  }

  public AgentSessionMeta toMeta(SessionId sessionId) {
    var session = sessionManager.getSession(sessionId);
    return new AgentSessionMeta(
        session.id(),
        session.title(),
        session.agentName(),
        stateService.getLatestRevision(sessionId),
        eventService.getEventCount(sessionId),
        session.lastUpdatedAt());
  }

  public List<AgentSessionMeta> listMetas() {
    return sessionManager.getAllSessions().stream()
        .map(s -> toMeta(s.id()))
        .sorted(Comparator.comparing(AgentSessionMeta::lastUpdatedAt).reversed())
        .toList();
  }
}
