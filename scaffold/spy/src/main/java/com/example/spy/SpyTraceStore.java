package com.example.spy;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bounded execution metadata only. No observation contexts, headers or credentials are retained.
 */
public class SpyTraceStore {
  private final int maxInvocations;
  private final int maxSpans;
  private final ArrayDeque<Invocation> invocations = new ArrayDeque<>();
  private long sequence;

  public SpyTraceStore(int maxInvocations, int maxSpans) {
    this.maxInvocations = maxInvocations;
    this.maxSpans = maxSpans;
  }

  synchronized Handle begin(Handle parent, Start start) {
    var invocation = parent == null ? new Invocation() : parent.invocation;
    if (invocation.spans.size() >= maxSpans) {
      invocation.truncated = true;
      return null;
    }
    var span = new Span(++sequence, parent == null ? null : parent.span.id, start);
    invocation.spans.add(span);
    if (parent == null) {
      invocations.addFirst(invocation);
      while (invocations.size() > maxInvocations) invocations.removeLast();
    }
    return new Handle(invocation, span);
  }

  synchronized void finish(Handle handle, String responseId, String outcome, String error) {
    if (handle == null) return;
    var span = handle.span;
    span.endSequence = ++sequence;
    span.durationMs = span.elapsedMillis();
    span.responseId = responseId;
    span.outcome = outcome;
    span.error = error;
  }

  public synchronized List<Summary> list() {
    return invocations.stream().map(Invocation::summary).toList();
  }

  public synchronized Optional<InvocationView> find(long id) {
    return invocations.stream()
        .filter(invocation -> invocation.spans.getFirst().id == id)
        .findFirst()
        .map(Invocation::view);
  }

  public synchronized void clear() {
    invocations.clear();
  }

  public record AdvisorInfo(String name, int order, String type) {}

  record Start(
      String kind,
      String name,
      Integer order,
      String prompt,
      int requestMessages,
      List<AdvisorInfo> advisors,
      String toolCallId) {
    Start {
      advisors = List.copyOf(advisors);
    }
  }

  public record Summary(
      long id,
      Instant startedAt,
      String prompt,
      long durationMs,
      boolean complete,
      String error,
      int spanCount,
      boolean truncated) {}

  public record SpanView(
      long id,
      Long parentId,
      String kind,
      String name,
      Integer order,
      long startSequence,
      Long endSequence,
      long durationMs,
      int requestMessages,
      List<AdvisorInfo> advisors,
      String toolCallId,
      String responseId,
      String outcome,
      String error) {}

  public record InvocationView(Summary summary, List<SpanView> spans) {}

  static final class Handle {
    private final Invocation invocation;
    private final Span span;

    Handle(Invocation invocation, Span span) {
      this.invocation = invocation;
      this.span = span;
    }
  }

  private static final class Invocation {
    private final List<Span> spans = new ArrayList<>();
    private boolean truncated;

    Summary summary() {
      var root = spans.getFirst();
      return new Summary(
          root.id,
          root.startedAt,
          root.start.prompt(),
          root.endSequence == null ? root.elapsedMillis() : root.durationMs,
          root.endSequence != null,
          root.error,
          spans.size(),
          truncated);
    }

    InvocationView view() {
      return new InvocationView(summary(), spans.stream().map(Span::view).toList());
    }
  }

  private static final class Span {
    private final long id;
    private final Long parentId;
    private final Start start;
    private final Instant startedAt = Instant.now();
    private final long startedNanos = System.nanoTime();
    private Long endSequence;
    private long durationMs;
    private String responseId;
    private String outcome;
    private String error;

    Span(long id, Long parentId, Start start) {
      this.id = id;
      this.parentId = parentId;
      this.start = start;
    }

    long elapsedMillis() {
      return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    SpanView view() {
      return new SpanView(
          id,
          parentId,
          start.kind(),
          start.name(),
          start.order(),
          id,
          endSequence,
          endSequence == null ? elapsedMillis() : durationMs,
          start.requestMessages(),
          start.advisors(),
          start.toolCallId(),
          responseId,
          outcome,
          error);
    }
  }
}
