package com.example.agent.core.session;

import com.example.agent.core.chat.ChatService;
import com.example.agent.core.event.EventService;
import com.example.agent.core.state.StateService;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class SessionManager {

  private final AtomicInteger sessionCounter = new AtomicInteger(0);
  private final ConcurrentHashMap<SessionId, Session> sessions = new ConcurrentHashMap<>();

  private final ChatService chatService;
  private final EventService eventService;
  private final StateService stateService;

  public SessionManager(
      ChatService chatService, EventService eventService, StateService stateService) {
    this.chatService = chatService;
    this.eventService = eventService;
    this.stateService = stateService;
  }

  public Session createSession(String agentName, String title) {
    var id = new SessionId(sessionCounter.incrementAndGet());
    var session = new Session(id, agentName, title, chatService, eventService, stateService);
    sessions.put(id, session);
    return session;
  }

  public Session getSession(SessionId sessionId) {
    var session = sessions.get(sessionId);
    if (session == null) {
      throw new SessionNotFoundException(sessionId);
    }
    return session;
  }

  public Collection<Session> getAllSessions() {
    return sessions.values();
  }
}
