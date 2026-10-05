package com.example.spy;

import java.io.IOException;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

/** Supplies library defaults without competing with the samples' application.yml files. */
public class SpyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    try {
      var defaults =
          new ResourcePropertySource("spy-defaults", "classpath:spy-defaults.properties");
      // Existing model integration tests run without a web server, so they cannot use loopback.
      if (application.getWebApplicationType() == WebApplicationType.NONE) {
        defaults.getSource().put("spring.ai.openai.base-url", "${spy.providers.openai}/v1");
      }
      environment.getPropertySources().addLast(defaults);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot load shared spy defaults", exception);
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
