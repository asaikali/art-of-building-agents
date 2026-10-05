package com.example.spy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.micrometer.observation.Observation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.prompt.Prompt;

class SpyTraceStoreTest {
  private static SpyTraceStore.Start start(String name) {
    return new SpyTraceStore.Start("client", name, null, name, 1, List.of(), null);
  }

  @Test
  void interleavedInvocationsKeepTheirOwnParentsAndExecutionOrder() {
    var store = new SpyTraceStore(2, 10);
    var first = store.begin(null, start("First"));
    var second = store.begin(null, start("Second"));
    var firstChild = store.begin(first, start("First child"));
    var secondChild = store.begin(second, start("Second child"));
    store.finish(secondChild, "second-response", "Response", null);
    store.finish(firstChild, "first-response", "Response", null);
    store.finish(first, null, "Response", null);
    store.finish(second, null, "Response", null);
    var firstView = store.find(store.list().getLast().id()).orElseThrow();
    var secondView = store.find(store.list().getFirst().id()).orElseThrow();
    assertThat(firstView.spans())
        .extracting(SpyTraceStore.SpanView::name)
        .containsExactly("First", "First child");
    assertThat(secondView.spans())
        .extracting(SpyTraceStore.SpanView::name)
        .containsExactly("Second", "Second child");
    assertThat(firstView.spans().getLast().parentId()).isEqualTo(firstView.summary().id());
    assertThat(firstView.spans().getLast().endSequence())
        .isLessThan(firstView.spans().getFirst().endSequence());
  }

  @Test
  void spansAndInvocationHistoryAreBoundedAndClearedActivityCannotReappear() {
    var store = new SpyTraceStore(1, 2);
    var root = store.begin(null, start("Root"));
    store.begin(root, start("Child"));
    assertThat(store.begin(root, start("Too many"))).isNull();
    assertThat(store.list().getFirst().truncated()).isTrue();
    store.begin(null, start("Newest"));
    assertThat(store.list()).hasSize(1);
    assertThat(store.list().getFirst().prompt()).isEqualTo("Newest");
    store.clear();
    store.finish(root, null, "Response", null);
    assertThat(store.list()).isEmpty();
  }

  @Test
  void errorsAreRecordedAsMetadataWithoutAnExceptionMessage() {
    var store = new SpyTraceStore(1, 2);
    var root = store.begin(null, start("Failure"));
    store.finish(root, null, null, "IllegalArgumentException");
    assertThat(store.list().getFirst().complete()).isTrue();
    assertThat(store.list().getFirst().error()).isEqualTo("IllegalArgumentException");
  }

  @Test
  void recordingFailuresNeverEscapeTheObservationHandler() {
    var store = mock(SpyTraceStore.class);
    doThrow(new IllegalStateException("recording failed")).when(store).begin(isNull(), any());
    doThrow(new IllegalStateException("recording failed"))
        .when(store)
        .finish(isNull(), isNull(), isNull(), isNull());
    var handler = new SpyObservationHandler(store);
    var context =
        ChatClientObservationContext.builder()
            .request(new ChatClientRequest(new Prompt("Test"), Map.of()))
            .advisors(List.of())
            .stream(false)
            .build();
    assertThatCode(
            () -> {
              handler.onStart(context);
              handler.onStop(context);
            })
        .doesNotThrowAnyException();
    assertThat(handler.supportsContext(new Observation.Context())).isFalse();
  }
}
