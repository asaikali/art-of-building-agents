package com.example.spy;

import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SpyController {
  private final SpyExchangeStore store;
  private final SpyTraceStore traces;
  private final boolean tracingEnabled;

  public SpyController(SpyExchangeStore store, SpyTraceStore traces, boolean tracingEnabled) {
    this.store = store;
    this.traces = traces;
    this.tracingEnabled = tracingEnabled;
  }

  @GetMapping(
      value = {"/", "/spy", "/spy/"},
      produces = MediaType.TEXT_HTML_VALUE)
  ResponseEntity<Resource> viewer() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(new ClassPathResource("spy/viewer.html"));
  }

  @GetMapping("/spy/api/exchanges")
  List<SpyExchangeStore.Summary> exchanges() {
    return store.list();
  }

  @GetMapping("/spy/api/exchanges/{id}")
  SpyExchangeStore.ExchangeView exchange(@PathVariable long id) {
    return store.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @DeleteMapping("/spy/api/exchanges")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void clear() {
    store.clear();
    traces.clear();
  }

  public record Invocations(boolean enabled, List<SpyTraceStore.Summary> items) {}

  @GetMapping("/spy/api/invocations")
  Invocations invocations() {
    return new Invocations(tracingEnabled, traces.list());
  }

  @GetMapping("/spy/api/invocations/{id}")
  SpyTraceStore.InvocationView invocation(@PathVariable long id) {
    return traces.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}
