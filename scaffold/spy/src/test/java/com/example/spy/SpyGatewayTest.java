package com.example.spy;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
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
    RECEIVED.set(null);
  }

  @Test
  void nativeSdkUsesLoopbackAndSharedModelDefault() throws InterruptedException {
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
    for (int i = 0; i < 6; i++) post("/spy/proxy/openai/v1/echo", "request-" + i);
    assertThat(store.list()).hasSize(4);
    var list = get("/spy/api/exchanges");
    assertThat(JSON.readTree(list.body()).size()).isEqualTo(4);
    long id = store.list().getFirst().id();
    assertThat(get("/spy/api/exchanges/" + id).body()).contains("request-5");
    assertThat(get("/spy").body())
        .contains("App → model", "Model → app")
        .doesNotContain("Request headers", "Response headers", "requestHeaders", "responseHeaders");
    assertThat(store.list()).hasSize(4);
    var deleted =
        CLIENT.send(
            HttpRequest.newBuilder(base.resolve("/spy/api/exchanges")).DELETE().build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(deleted.statusCode()).isEqualTo(204);
    assertThat(store.list()).isEmpty();
    assertThat(get("/spy/api/exchanges/" + id).statusCode()).isEqualTo(404);
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
    } else {
      int status = 200;
      String result = body;
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.getResponseHeaders().set("Set-Cookie", "cookie-secret");
      exchange.getResponseHeaders().set("X-Custom-Credential", "custom-key");
      if (exchange.getRequestURI().getPath().endsWith("/chat/completions")) {
        result =
            "{\"id\":\"demo\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"gpt-5.4-mini\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"Stub answer\"},\"finish_reason\":\"stop\"}]}";
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

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  static class TestApp {}
}
