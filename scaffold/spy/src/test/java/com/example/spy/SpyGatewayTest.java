package com.example.spy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

class SpyGatewayTest {
  private static final HttpClient CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private static final JsonMapper JSON = new JsonMapper();
  private static final AtomicReference<Received> RECEIVED = new AtomicReference<>();
  private static final CountDownLatch RELEASE_STREAM = new CountDownLatch(1);
  private static final String FIRST_CHUNK = "data: {\"chunk\":1}\n\n";
  private static final String SECOND_CHUNK = "data: [DONE]\n\n";
  private static HttpServer upstream;
  private static ConfigurableApplicationContext app;
  private static URI base;
  private static SpyExchangeStore store;
  private static SpyTraceStore traces;

  @BeforeAll
  static void start() throws IOException {
    upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    upstream.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    upstream.createContext("/", SpyGatewayTest::respond);
    upstream.start();
    int port;
    try (var socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    int unavailablePort;
    try (var socket = new ServerSocket(0)) {
      unavailablePort = socket.getLocalPort();
    }
    base = URI.create("http://127.0.0.1:" + port);
    app =
        new SpringApplicationBuilder(TestApp.class)
            .run(
                "--server.port=" + port,
                "--spring.ai.openai.api-key=spy-test-key",
                "--spring.ai.openai.max-retries=0",
                "--spy.providers.openai=http://127.0.0.1:" + upstream.getAddress().getPort(),
                "--spy.providers.other=http://127.0.0.1:" + upstream.getAddress().getPort(),
                "--spy.providers.unavailable=http://127.0.0.1:" + unavailablePort,
                "--spy.max-exchanges=4",
                "--spy.max-body-bytes=1024");
    store = app.getBean(SpyExchangeStore.class);
    traces = app.getBean(SpyTraceStore.class);
  }

  @AfterAll
  static void stop() {
    RELEASE_STREAM.countDown();
    if (app != null) app.close();
    if (upstream != null) upstream.stop(0);
  }

  @BeforeEach
  void clear() {
    store.clear();
    traces.clear();
    RECEIVED.set(null);
  }

  @Test
  void nativeSdkUsesLoopbackAndSharedModelDefault() throws Exception {
    var result =
        app.getBean(ChatClient.Builder.class)
            .build()
            .prompt()
            .user("Explain user prompts")
            .call()
            .content();
    assertThat(result).isEqualTo("Stub answer");
    awaitComplete();
    assertThat(RECEIVED.get().path()).isEqualTo("/v1/chat/completions");
    assertThat(RECEIVED.get().authorization()).isEqualTo("Bearer spy-test-key");
    var body = JSON.readTree(RECEIVED.get().body());
    assertThat(body.get("model").asText()).isEqualTo("gpt-5.4-mini");
    assertThat(body.get("messages").get(0).get("content").asText())
        .isEqualTo("Explain user prompts");
    var capture = latest();
    assertThat(capture.requestBody().text()).isEqualTo(RECEIVED.get().body());
    assertThat(capture.responseBody().text()).contains("Stub answer");
    assertThat(JSON.writeValueAsString(capture)).doesNotContain("spy-test-key");
    assertThat(traces.list()).hasSize(1);
    var trace = traces.find(traces.list().getFirst().id()).orElseThrow();
    assertThat(trace.summary().complete()).isTrue();
    assertThat(trace.spans())
        .anyMatch(span -> span.kind().equals("model") && "demo".equals(span.responseId()));
    assertThat(get("/spy/api/invocations").body()).contains("\"enabled\":true");
    assertThat(JSON.writeValueAsString(trace)).doesNotContain("spy-test-key");
  }

  @Test
  void observesAdvisorOrderAndRepeatedToolRoundsWithoutChangingTheResult() throws Exception {
    var outer = new CountingAdvisor("Turn", ToolCallingAdvisor.DEFAULT_ORDER - 1);
    var inner = new CountingAdvisor("Model", ToolCallingAdvisor.DEFAULT_ORDER + 1);
    var tools = new LoopTools();
    var result =
        app.getBean(ChatClient.Builder.class)
            .clone()
            .defaultAdvisors(
                outer,
                ToolCallingAdvisor.builder()
                    .toolCallingManager(app.getBean(ToolCallingManager.class))
                    .build(),
                inner)
            .build()
            .prompt()
            .tools(tools)
            .user("spy-tool-loop")
            .call()
            .content();
    assertThat(result).isEqualTo("Loop complete");
    assertThat(outer.calls.get()).isEqualTo(1);
    assertThat(inner.calls.get()).isEqualTo(3);
    assertThat(tools.calls.get()).isEqualTo(2);
    assertThat(store.list()).hasSize(3);
    assertThat(traces.list()).hasSize(1);
    var trace = traces.find(traces.list().getFirst().id()).orElseThrow();
    assertThat(trace.summary().complete()).isTrue();
    assertThat(trace.spans().stream().filter(span -> span.name().equals("Turn"))).hasSize(1);
    assertThat(trace.spans().stream().filter(span -> span.name().equals("Model"))).hasSize(3);
    assertThat(trace.spans().stream().filter(span -> span.kind().equals("model")))
        .extracting(SpyTraceStore.SpanView::responseId)
        .containsExactly("loop-1", "loop-2", "loop-3");
    assertThat(trace.spans().stream().filter(span -> span.kind().equals("tool")))
        .extracting(SpyTraceStore.SpanView::toolCallId)
        .containsExactly("weather-call", "activity-call");
    assertThat(trace.spans().getFirst().advisors())
        .extracting(SpyTraceStore.AdvisorInfo::name)
        .containsSubsequence("Turn", "Tool Calling Advisor", "Model");
    var detail = get("/spy/api/invocations/" + trace.summary().id());
    assertThat(detail.statusCode()).isEqualTo(200);
    assertThat(detail.body()).doesNotContain("spy-test-key", "toolCallArguments", "toolCallResult");
  }

  @Test
  void observesNativeStreamingWithTheSameParentChain() throws InterruptedException {
    var result =
        app.getBean(ChatClient.Builder.class).build().prompt().user("spy-native-stream").stream()
            .content()
            .collectList()
            .block(Duration.ofSeconds(10));
    assertThat(result).containsExactly("Streaming answer");
    // Reactor's doFinally observations stop after the subscriber receives onComplete.
    for (int i = 0; i < 100; i++) {
      var view = traces.find(traces.list().getFirst().id()).orElseThrow();
      if (view.spans().stream().allMatch(span -> span.endSequence() != null)) break;
      Thread.sleep(10);
    }
    var trace = traces.find(traces.list().getFirst().id()).orElseThrow();
    assertThat(traces.list()).hasSize(1);
    assertThat(trace.spans().stream().filter(span -> span.kind().equals("model")))
        .allSatisfy(
            span -> {
              assertThat(span.parentId()).isNotNull();
              assertThat(span.responseId()).isEqualTo("native-stream");
            });
  }

  @Test
  void capturesNestedChatClientsInsideAToolAsOneInvocation() {
    var nested = app.getBean(ChatClient.Builder.class).clone().build();
    var result =
        app.getBean(ChatClient.Builder.class)
            .clone()
            .defaultAdvisors(
                ToolCallingAdvisor.builder()
                    .toolCallingManager(app.getBean(ToolCallingManager.class))
                    .build())
            .build()
            .prompt()
            .tools(new LoopTools(nested))
            .user("spy-tool-loop")
            .call()
            .content();
    assertThat(result).isEqualTo("Loop complete");
    assertThat(traces.list()).hasSize(1);
    var trace = traces.find(traces.list().getFirst().id()).orElseThrow();
    assertThat(trace.spans().stream().filter(span -> span.kind().equals("client"))).hasSize(2);
    var child =
        trace.spans().stream()
            .filter(span -> span.kind().equals("client") && span.parentId() != null)
            .findFirst()
            .orElseThrow();
    assertThat(
            trace.spans().stream()
                .filter(span -> span.id() == child.parentId())
                .findFirst()
                .orElseThrow()
                .kind())
        .isEqualTo("tool");
    assertThat(trace.spans().stream().filter(span -> span.kind().equals("model"))).hasSize(4);
  }

  @Test
  void anAdvisorCanShortCircuitWithoutSpyInvokingTheRemainingChain() {
    CallAdvisor cached =
        new CallAdvisor() {
          public String getName() {
            return "Cache";
          }

          public int getOrder() {
            return 0;
          }

          public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            return ChatClientResponse.builder()
                .chatResponse(
                    new ChatResponse(
                        java.util.List.of(new Generation(new AssistantMessage("Cached answer")))))
                .build();
          }
        };
    var result =
        app.getBean(ChatClient.Builder.class)
            .clone()
            .defaultAdvisors(cached)
            .build()
            .prompt()
            .user("Cache hit")
            .call()
            .content();
    assertThat(result).isEqualTo("Cached answer");
    assertThat(store.list()).isEmpty();
    var trace = traces.find(traces.list().getFirst().id()).orElseThrow();
    assertThat(trace.spans())
        .noneMatch(span -> span.kind().equals("model") || span.kind().equals("tool"));
    assertThat(trace.spans())
        .extracting(SpyTraceStore.SpanView::name)
        .contains("Cache")
        .doesNotContain("call");
    assertThat(trace.spans().getFirst().advisors())
        .extracting(SpyTraceStore.AdvisorInfo::name)
        .contains("Cache", "call");
  }

  @Test
  void tracingCanBeDisabledWithoutRemovingTheGateway() {
    try (var disabled =
        new SpringApplicationBuilder(TestApp.class)
            .run(
                "--server.port=0",
                "--spring.ai.openai.api-key=unused",
                "--spy.tracing.enabled=false")) {
      assertThat(disabled.getBeansOfType(SpyObservationHandler.class)).isEmpty();
      assertThat(disabled.getBean(SpyController.class).invocations().enabled()).isFalse();
      assertThat(disabled.getBean(SpyExchangeStore.class)).isNotNull();
    }
  }

  @Test
  void advisorErrorsKeepTheirOriginalExceptionWithoutRecordingItsMessage() {
    CallAdvisor failed =
        new CallAdvisor() {
          public String getName() {
            return "Failure";
          }

          public int getOrder() {
            return 0;
          }

          public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            throw new IllegalStateException("private-error-detail");
          }
        };
    var client = app.getBean(ChatClient.Builder.class).clone().defaultAdvisors(failed).build();
    assertThatThrownBy(() -> client.prompt().user("Fail this invocation").call().content())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("private-error-detail");
    var trace = traces.find(traces.list().getFirst().id()).orElseThrow();
    assertThat(trace.summary().error()).isEqualTo("IllegalStateException");
    assertThat(JSON.writeValueAsString(trace)).doesNotContain("private-error-detail");
    assertThat(store.list()).isEmpty();
  }

  @Test
  void forwardsPathsQueriesHeadersAndBodiesForAnyConfiguredProvider() throws Exception {
    String body = "{\"messages\":[{\"role\":\"user\",\"content\":\"café 🥕\"}]}";
    var response = post("/spy/proxy/other/v1/echo?mode=demo", body);
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo(body);
    assertThat(RECEIVED.get())
        .isEqualTo(new Received("/v1/echo?mode=demo", "Bearer secret", "private", body));
    var capture = latest();
    assertThat(capture.summary().destination()).endsWith("/v1/echo?mode=demo");
    assertThat(capture.summary().complete()).isTrue();
    assertThat(capture.requestBody().text()).isEqualTo(body);
    assertThat(capture.responseBody().text()).isEqualTo(body);
    var detail = get("/spy/api/exchanges/" + capture.summary().id()).body();
    assertThat(JSON.readTree(detail).has("requestHeaders")).isFalse();
    assertThat(JSON.readTree(detail).has("responseHeaders")).isFalse();
    assertThat(detail).doesNotContain("secret", "private", "cookie-secret", "custom-key");
  }

  @Test
  void preservesProviderErrorsAndNonJsonResponses() throws Exception {
    var response = post("/spy/proxy/openai/v1/error", "bad request");
    assertThat(response.statusCode()).isEqualTo(429);
    assertThat(response.headers().firstValue("retry-after")).contains("5");
    assertThat(response.body()).isEqualTo("Provider is busy");
    assertThat(latest().summary().status()).isEqualTo(429);
    assertThat(latest().responseBody().text()).isEqualTo("Provider is busy");
  }

  @Test
  void capturesConnectionFailuresAsGatewayErrors() throws Exception {
    var response = post("/spy/proxy/unavailable/v1/echo", "test");
    assertThat(response.statusCode()).isEqualTo(502);
    assertThat(response.body()).contains("The model provider request failed.");
    assertThat(latest().summary().status()).isEqualTo(502);
    assertThat(latest().responseBody().text()).isEqualTo(response.body());
  }

  @Test
  void captureLimitDoesNotTruncateForwardedRequestOrResponse() throws Exception {
    String body = "x".repeat(4096);
    var response = post("/spy/proxy/openai/v1/echo", body);
    assertThat(response.body()).isEqualTo(body);
    assertThat(RECEIVED.get().body()).isEqualTo(body);
    var capture = latest();
    assertThat(capture.requestBody().text()).hasSize(1024);
    assertThat(capture.requestBody().totalBytes()).isEqualTo(4096);
    assertThat(capture.requestBody().truncated()).isTrue();
    assertThat(capture.responseBody().text()).hasSize(1024);
    assertThat(capture.responseBody().truncated()).isTrue();
  }

  @Test
  void displaysGzipAsTextWhileForwardingTheOriginalCompressedBytes() throws Exception {
    var response =
        CLIENT.send(
            HttpRequest.newBuilder(base.resolve("/spy/proxy/openai/v1/gzip"))
                .header("Accept-Encoding", "gzip")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());
    assertThat(response.headers().firstValue("content-encoding")).contains("gzip");
    try (var decoded = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
      assertThat(new String(decoded.readAllBytes(), StandardCharsets.UTF_8))
          .isEqualTo("{\"answer\":\"compressed\"}");
    }
    awaitComplete();
    assertThat(latest().responseBody().text()).isEqualTo("{\"answer\":\"compressed\"}");
    assertThat(latest().responseBody().totalBytes()).isEqualTo(response.body().length);
  }

  @Test
  void streamsFirstChunkBeforeUpstreamFinishes() throws Exception {
    var request =
        HttpRequest.newBuilder(base.resolve("/spy/proxy/openai/v1/stream"))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
    try (var body = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
      try {
        var first =
            CompletableFuture.supplyAsync(
                    () -> {
                      try {
                        return new String(
                            body.readNBytes(FIRST_CHUNK.length()), StandardCharsets.UTF_8);
                      } catch (IOException exception) {
                        throw new RuntimeException(exception);
                      }
                    })
                .get(5, TimeUnit.SECONDS);
        assertThat(first).isEqualTo(FIRST_CHUNK);
        assertThat(latest().summary().complete()).isFalse();
      } finally {
        RELEASE_STREAM.countDown();
      }
      assertThat(new String(body.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(SECOND_CHUNK);
    }
    awaitComplete();
    assertThat(latest().responseBody().text()).isEqualTo(FIRST_CHUNK + SECOND_CHUNK);
  }

  @Test
  void viewerApiDoesNotCaptureItselfAndHistoryIsBoundedAndClearable() throws Exception {
    app.getBean(ChatClient.Builder.class).build().prompt().user("Trace to clear").call().content();
    assertThat(traces.list()).hasSize(1);
    long invocationId = traces.list().getFirst().id();
    for (int i = 0; i < 6; i++) post("/spy/proxy/openai/v1/echo", "request-" + i);
    assertThat(store.list()).hasSize(4);
    var list = get("/spy/api/exchanges");
    assertThat(JSON.readTree(list.body()).size()).isEqualTo(4);
    long id = store.list().getFirst().id();
    assertThat(get("/spy/api/exchanges/" + id).body()).contains("request-5");
    assertThat(get("/spy").body())
        .contains("Request: App → Model", "Response: Model → App")
        .doesNotContain("Request headers", "Response headers", "requestHeaders", "responseHeaders");
    var root = get("/");
    assertThat(root.statusCode()).isEqualTo(200);
    assertThat(root.headers().firstValue("cache-control")).contains("no-store");
    assertThat(root.body()).isEqualTo(get("/spy").body());
    assertThat(get("/spy/").body()).isEqualTo(root.body());
    var turnsScript = get("/spy/turns.js");
    assertThat(turnsScript.statusCode()).isEqualTo(200);
    assertThat(turnsScript.headers().firstValue("content-type").orElseThrow())
        .contains("javascript");
    assertThat(get("/spy/advisor-flow.js").statusCode()).isEqualTo(200);
    assertThat(store.list()).hasSize(4);
    var deleted =
        CLIENT.send(
            HttpRequest.newBuilder(base.resolve("/spy/api/exchanges")).DELETE().build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(deleted.statusCode()).isEqualTo(204);
    assertThat(store.list()).isEmpty();
    assertThat(traces.list()).isEmpty();
    assertThat(get("/spy/api/exchanges/" + id).statusCode()).isEqualTo(404);
    assertThat(get("/spy/api/invocations/" + invocationId).statusCode()).isEqualTo(404);
  }

  private static HttpResponse<String> post(String path, String body) throws Exception {
    var response =
        CLIENT.send(
            HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer secret")
                .header("X-Api-Key", "private")
                .header("X-Custom-Credential", "custom-key")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    awaitComplete();
    return response;
  }

  private static HttpResponse<String> get(String path) throws Exception {
    return CLIENT.send(
        HttpRequest.newBuilder(base.resolve(path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static SpyExchangeStore.ExchangeView latest() {
    return store.find(store.list().getFirst().id()).orElseThrow();
  }

  private static void awaitComplete() throws InterruptedException {
    for (int i = 0; i < 100 && !latest().summary().complete(); i++) Thread.sleep(10);
    assertThat(latest().summary().complete()).isTrue();
  }

  private static void respond(HttpExchange exchange) throws IOException {
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    RECEIVED.set(
        new Received(
            exchange.getRequestURI().toString(),
            exchange.getRequestHeaders().getFirst("Authorization"),
            exchange.getRequestHeaders().getFirst("X-Api-Key"),
            body));
    if (exchange.getRequestURI().getPath().endsWith("/stream")) {
      exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
      exchange.sendResponseHeaders(200, 0);
      exchange.getResponseBody().write(FIRST_CHUNK.getBytes(StandardCharsets.UTF_8));
      exchange.getResponseBody().flush();
      try {
        RELEASE_STREAM.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
      exchange.getResponseBody().write(SECOND_CHUNK.getBytes(StandardCharsets.UTF_8));
    } else if (body.contains("spy-native-stream")) {
      exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
      exchange.sendResponseHeaders(200, 0);
      String chunks =
          "data: {\"id\":\"native-stream\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-5.4-mini\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"Streaming answer\"},\"finish_reason\":null}]}\n\n"
              + "data: {\"id\":\"native-stream\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-5.4-mini\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
      exchange.getResponseBody().write(chunks.getBytes(StandardCharsets.UTF_8));
    } else {
      int status = 200;
      String result = body;
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.getResponseHeaders().set("Set-Cookie", "cookie-secret");
      exchange.getResponseHeaders().set("X-Custom-Credential", "custom-key");
      if (exchange.getRequestURI().getPath().endsWith("/chat/completions")) {
        result =
            "{\"id\":\"demo\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"gpt-5.4-mini\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"Stub answer\"},\"finish_reason\":\"stop\"}]}";
        if (body.contains("spy-tool-loop")) {
          boolean weather = body.contains("\"tool_call_id\":\"weather-call\"");
          boolean activity = body.contains("\"tool_call_id\":\"activity-call\"");
          String message =
              activity
                  ? "{\"role\":\"assistant\",\"content\":\"Loop complete\"}"
                  : "{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\""
                      + (weather ? "activity-call" : "weather-call")
                      + "\",\"type\":\"function\",\"function\":{\"name\":\""
                      + (weather ? "activities" : "weather")
                      + "\",\"arguments\":\"{}\"}}]}";
          result =
              "{\"id\":\"loop-"
                  + (activity ? 3 : weather ? 2 : 1)
                  + "\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"gpt-5.4-mini\",\"choices\":[{\"index\":0,\"message\":"
                  + message
                  + ",\"finish_reason\":\""
                  + (activity ? "stop" : "tool_calls")
                  + "\"}]}";
        }
      } else if (exchange.getRequestURI().getPath().endsWith("/error")) {
        status = 429;
        result = "Provider is busy";
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.getResponseHeaders().set("Retry-After", "5");
      }
      byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
      if (exchange.getRequestURI().getPath().endsWith("/gzip")) {
        bytes = "{\"answer\":\"compressed\"}".getBytes(StandardCharsets.UTF_8);
      }
      if (exchange.getRequestURI().getPath().endsWith("/gzip")
          || "gzip".equals(exchange.getRequestHeaders().getFirst("Accept-Encoding"))) {
        var compressed = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(compressed)) {
          gzip.write(bytes);
        }
        bytes = compressed.toByteArray();
        exchange.getResponseHeaders().set("Content-Encoding", "gzip");
      }
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
    }
    exchange.close();
  }

  record Received(String path, String authorization, String apiKey, String body) {}

  static class CountingAdvisor implements CallAdvisor {
    final AtomicInteger calls = new AtomicInteger();
    final String name;
    final int order;

    CountingAdvisor(String name, int order) {
      this.name = name;
      this.order = order;
    }

    public String getName() {
      return name;
    }

    public int getOrder() {
      return order;
    }

    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
      calls.incrementAndGet();
      return chain.nextCall(request);
    }
  }

  static class LoopTools {
    final AtomicInteger calls = new AtomicInteger();
    final ChatClient nested;

    LoopTools() {
      this(null);
    }

    LoopTools(ChatClient nested) {
      this.nested = nested;
    }

    @Tool(description = "Get weather")
    public String weather() {
      calls.incrementAndGet();
      if (nested != null) return nested.prompt().user("Explain user prompts").call().content();
      return "Sunny";
    }

    @Tool(description = "Find activities")
    public String activities() {
      calls.incrementAndGet();
      return "Walk";
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  static class TestApp {}
}
