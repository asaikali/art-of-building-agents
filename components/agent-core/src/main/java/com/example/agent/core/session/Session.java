package com.example.agent.core.session;

import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.ChatService;
import com.example.agent.core.chat.Role;
import com.example.agent.core.event.AgentEvent;
import com.example.agent.core.event.EventService;
import com.example.agent.core.state.AgentStateRevision;
import com.example.agent.core.state.StateService;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Facade giving an agent a single object to interact with. Created by {@link SessionManager};
 * delegates to the feature services internally.
 */
public class Session {

  private final SessionId sessionId;
  private final String agentName;
  private final String title;
  private final Instant createdAt;
  private volatile Instant lastUpdatedAt;

  private final ChatService chatService;
  private final EventService eventService;
  private final StateService stateService;

  Session(
      SessionId sessionId,
      String agentName,
      String title,
      ChatService chatService,
      EventService eventService,
      StateService stateService) {
    this.sessionId = sessionId;
    this.agentName = agentName;
    this.title = title;
    this.createdAt = Instant.now();
    this.lastUpdatedAt = this.createdAt;
    this.chatService = chatService;
    this.eventService = eventService;
    this.stateService = stateService;
  }

  // ── facade methods ──────────────────────────────────────────────────

  public AgentMessage appendMessage(Role role, String text) {
    var msg = chatService.appendMessage(sessionId, role, text);
    touch();
    return msg;
  }

  public List<AgentMessage> getMessages() {
    return chatService.getMessages(sessionId);
  }

  public AgentEvent logEvent(String msg, Map<String, Object> data) {
    var event = eventService.appendEvent(sessionId, msg, data);
    touch();
    return event;
  }

  public AgentStateRevision updateState(String markdown) {
    var revision = stateService.setState(sessionId, markdown);
    touch();
    return revision;
  }

  // ── metadata ────────────────────────────────────────────────────────

  public SessionId id() {
    return sessionId;
  }

  public String agentName() {
    return agentName;
  }

  public String title() {
    return title;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant lastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void touch() {
    this.lastUpdatedAt = Instant.now();
  }
}
