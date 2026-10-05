package com.example.spy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

/** Bounded in-memory history. Capturing fewer bytes never changes the bytes forwarded. */
public class SpyExchangeStore {
  private final int maxExchanges;
  private final int maxBodyBytes;
  private final AtomicLong sequence = new AtomicLong();
  private final ArrayDeque<Exchange> exchanges = new ArrayDeque<>();

  public SpyExchangeStore(int maxExchanges, int maxBodyBytes) {
    this.maxExchanges = maxExchanges;
    this.maxBodyBytes = maxBodyBytes;
  }

  synchronized Exchange begin(
      String method, String destination, Map<String, List<String>> requestHeaders) {
    var exchange =
        new Exchange(sequence.incrementAndGet(), method, destination, requestHeaders, maxBodyBytes);
    exchanges.addFirst(exchange);
    while (exchanges.size() > maxExchanges) {
      exchanges.removeLast();
    }
    return exchange;
  }

  public synchronized List<Summary> list() {
    return exchanges.stream().map(Exchange::summary).toList();
  }

  public synchronized Optional<ExchangeView> find(long id) {
    return exchanges.stream().filter(exchange -> exchange.id == id).findFirst().map(Exchange::view);
  }

  public synchronized void clear() {
    exchanges.clear();
  }

  public record Summary(
      long id,
      Instant startedAt,
      String method,
      String destination,
      int status,
      long durationMs,
      boolean complete,
      String error) {}

  public record Body(String text, long totalBytes, boolean truncated) {}

  public record ExchangeView(
      Summary summary,
      Map<String, List<String>> requestHeaders,
      Body requestBody,
      Map<String, List<String>> responseHeaders,
      Body responseBody) {}

  static final class Exchange {
    private final long id;
    private final Instant startedAt = Instant.now();
    private final long startedNanos = System.nanoTime();
    private final String method;
    private final String destination;
    private final Map<String, List<String>> requestHeaders;
    final BodyCapture requestBody;
    final BodyCapture responseBody;
    private Map<String, List<String>> responseHeaders = Map.of();
    private int status;
    private long durationMs;
    private boolean complete;
    private String error;

    Exchange(
        long id,
        String method,
        String destination,
        Map<String, List<String>> requestHeaders,
        int limit) {
      this.id = id;
      this.method = method;
      this.destination = destination;
      this.requestHeaders = Map.copyOf(requestHeaders);
      requestBody = new BodyCapture(limit);
      responseBody = new BodyCapture(limit);
    }

    synchronized void finish(int status, Map<String, List<String>> headers, String error) {
      this.status = status;
      responseHeaders = Map.copyOf(headers);
      this.error = error;
      durationMs = elapsedMillis();
      complete = true;
    }

    synchronized Summary summary() {
      return new Summary(
          id,
          startedAt,
          method,
          destination,
          status,
          complete ? durationMs : elapsedMillis(),
          complete,
          error);
    }

    synchronized ExchangeView view() {
      return new ExchangeView(
          summary(),
          requestHeaders,
          requestBody.snapshot(),
          responseHeaders,
          responseBody.snapshot(responseHeaders, complete));
    }

    private long elapsedMillis() {
      return (System.nanoTime() - startedNanos) / 1_000_000;
    }
  }

  static final class BodyCapture {
    private final int limit;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private long totalBytes;

    BodyCapture(int limit) {
      this.limit = limit;
    }

    synchronized void append(byte[] buffer, int offset, int length) {
      bytes.write(buffer, offset, Math.min(length, limit - bytes.size()));
      totalBytes += length;
    }

    synchronized void append(int value) {
      if (bytes.size() < limit) {
        bytes.write(value);
      }
      totalBytes++;
    }

    synchronized Body snapshot() {
      return new Body(
          bytes.toString(StandardCharsets.UTF_8), totalBytes, totalBytes > bytes.size());
    }

    synchronized Body snapshot(Map<String, List<String>> headers, boolean complete) {
      boolean gzip =
          headers.entrySet().stream()
              .filter(entry -> entry.getKey().equalsIgnoreCase("Content-Encoding"))
              .flatMap(entry -> entry.getValue().stream())
              .anyMatch(value -> value.equalsIgnoreCase("gzip"));
      if (!gzip) return snapshot();
      boolean truncated = totalBytes > bytes.size();
      if (!complete || truncated) {
        return new Body(
            truncated
                ? "(Compressed body exceeded the capture limit.)"
                : "(Compressed response is still being received.)",
            totalBytes,
            truncated);
      }
      try (var decoded = new GZIPInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
        byte[] body = decoded.readNBytes(limit + 1);
        return new Body(
            new String(body, 0, Math.min(body.length, limit), StandardCharsets.UTF_8),
            totalBytes,
            body.length > limit);
      } catch (IOException exception) {
        return new Body("(Unable to decode gzip response.)", totalBytes, false);
      }
    }
  }
}
