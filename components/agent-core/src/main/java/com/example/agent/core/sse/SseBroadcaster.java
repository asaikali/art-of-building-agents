package com.example.agent.core.sse;

import com.example.agent.core.session.SessionId;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Generic SSE subscriber management: history replay + live broadcast. */
public class SseBroadcaster<T> {

  private final ConcurrentHashMap<SessionId, CopyOnWriteArrayList<SseEmitter>> subscribers =
      new ConcurrentHashMap<>();

  /** Replay history items then register the emitter for live updates. */
  public SseEmitter subscribe(SessionId key, Collection<T> history) {
    var emitter = new SseEmitter(Long.MAX_VALUE);
    var list = subscribers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());

    for (T item : history) {
      try {
        emitter.send(SseEmitter.event().data(item));
      } catch (IOException e) {
        emitter.completeWithError(e);
        return emitter;
      }
    }

    list.add(emitter);
    emitter.onCompletion(() -> list.remove(emitter));
    emitter.onTimeout(() -> list.remove(emitter));
    emitter.onError(e -> list.remove(emitter));

    return emitter;
  }

  /** Broadcast a single item to all subscribers for the given key. */
  public void broadcast(SessionId key, T item) {
    var list = subscribers.get(key);
    if (list == null) {
      return;
    }
    for (var emitter : list) {
      try {
        emitter.send(SseEmitter.event().data(item));
      } catch (IOException e) {
        list.remove(emitter);
      }
    }
  }
}
