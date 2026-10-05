package com.example.spy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

class SpyEnvironmentPostProcessorTest {
  @Test
  void defaultsApplyAndPortOverrideMovesLoopback() {
    var environment = new MockEnvironment();
    var application = new SpringApplication();
    application.setWebApplicationType(WebApplicationType.SERVLET);
    new SpyEnvironmentPostProcessor().postProcessEnvironment(environment, application);

    assertThat(environment.getProperty("server.port")).isEqualTo("8080");
    assertThat(environment.getProperty("spring.ai.openai.base-url"))
        .isEqualTo("http://127.0.0.1:8080/spy/proxy/openai/v1");

    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "sample",
                Map.of(
                    "server.port",
                    "9090",
                    "spring.ai.openai.chat.model",
                    "sample-model",
                    "OPENAI_API_KEY",
                    "sample-key")));
    assertThat(environment.getProperty("spring.ai.openai.base-url"))
        .isEqualTo("http://127.0.0.1:9090/spy/proxy/openai/v1");
    assertThat(environment.getProperty("spring.ai.openai.chat.model")).isEqualTo("sample-model");
    assertThat(environment.getProperty("spring.ai.openai.api-key")).isEqualTo("sample-key");

    environment.setProperty("spring.ai.openai.base-url", "https://example.test/v1");
    assertThat(environment.getProperty("spring.ai.openai.base-url"))
        .isEqualTo("https://example.test/v1");
  }

  @Test
  void nonWebApplicationsUseUpstreamWithoutAnEmbeddedServer() {
    var environment = new MockEnvironment();
    var application = new SpringApplication();
    application.setWebApplicationType(WebApplicationType.NONE);
    new SpyEnvironmentPostProcessor().postProcessEnvironment(environment, application);
    assertThat(environment.getProperty("spring.ai.openai.base-url"))
        .isEqualTo("https://api.openai.com/v1");
  }
}
