package com.example.agent.core.event;

import com.example.agent.core.session.SessionId;
import com.example.agent.core.sse.SseBroadcaster;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class EventService {

  private final ConcurrentHashMap<SessionId, CopyOnWriteArrayList<AgentEvent>> eventStore =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<SessionId, AtomicLong> seqCounters = new ConcurrentHashMap<>();
  private final SseBroadcaster<AgentEvent> broadcaster = new SseBroadcaster<>();

  public AgentEvent appendEvent(SessionId sessionId, String msg, Map<String, Object> data) {
    long seq = seqCounters.computeIfAbsent(sessionId, k -> new AtomicLong(1)).getAndIncrement();
    var event = new AgentEvent(Instant.now(), seq, msg, data);
    eventStore.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(event);
    broadcaster.broadcast(sessionId, event);
    return event;
  }

  public SseEmitter streamEvents(SessionId sessionId) {
    var events = eventStore.get(sessionId);
    var history = events == null ? List.<AgentEvent>of() : events;
    return broadcaster.subscribe(sessionId, history);
  }

  public long getEventCount(SessionId sessionId) {
    var events = eventStore.get(sessionId);
    return events == null ? 0 : events.size();
  }
}
