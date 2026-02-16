package com.example.agent.core.state;

import com.example.agent.core.session.SessionId;
import com.example.agent.core.sse.SseBroadcaster;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class StateService {

  private final ConcurrentHashMap<SessionId, NavigableMap<Long, AgentStateRevision>> stateStore =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<SessionId, AtomicLong> revCounters = new ConcurrentHashMap<>();
  private final SseBroadcaster<AgentStateRevision> broadcaster = new SseBroadcaster<>();

  public AgentStateRevision setState(SessionId sessionId, String markdown) {
    long rev = revCounters.computeIfAbsent(sessionId, k -> new AtomicLong(1)).getAndIncrement();
    var revision = new AgentStateRevision(Instant.now(), rev, sessionId, markdown);
    stateStore
        .computeIfAbsent(sessionId, k -> Collections.synchronizedNavigableMap(new TreeMap<>()))
        .put(rev, revision);
    broadcaster.broadcast(sessionId, revision);
    return revision;
  }

  public AgentStateRevision getStateRevision(SessionId sessionId, long rev) {
    var revisions = stateStore.get(sessionId);
    var revision = revisions == null ? null : revisions.get(rev);
    if (revision == null) {
      throw new RevisionNotFoundException(sessionId, rev);
    }
    return revision;
  }

  public SseEmitter streamState(SessionId sessionId) {
    var revisions = stateStore.get(sessionId);
    var history = revisions == null ? List.<AgentStateRevision>of() : revisions.values();
    return broadcaster.subscribe(sessionId, history);
  }

  public long getLatestRevision(SessionId sessionId) {
    var counter = revCounters.get(sessionId);
    long next = counter == null ? 1 : counter.get();
    return Math.max(0, next - 1);
  }
}
