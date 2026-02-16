package com.example.agent.core.chat;

import com.example.agent.core.session.SessionId;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

  private final ConcurrentHashMap<SessionId, CopyOnWriteArrayList<AgentMessage>> messageStore =
      new ConcurrentHashMap<>();

  public List<AgentMessage> getMessages(SessionId sessionId) {
    var messages = messageStore.get(sessionId);
    return messages == null ? List.of() : Collections.unmodifiableList(messages);
  }

  public AgentMessage appendMessage(SessionId sessionId, Role role, String text) {
    var msg = new AgentMessage(Instant.now(), role, text);
    messageStore.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(msg);
    return msg;
  }
}
