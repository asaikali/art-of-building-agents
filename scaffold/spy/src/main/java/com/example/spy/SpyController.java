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

  public SpyController(SpyExchangeStore store) {
    this.store = store;
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
  }
}
