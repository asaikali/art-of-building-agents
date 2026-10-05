package com.example.spy;

import java.net.URI;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spy")
public record SpyProperties(
    Map<String, URI> providers, int maxExchanges, int maxBodyBytes, Tracing tracing) {
  public SpyProperties {
    providers = Map.copyOf(providers);
    if (maxExchanges < 1 || maxBodyBytes < 1) {
      throw new IllegalArgumentException("Spy capture limits must be positive");
    }
    providers.forEach(
        (name, uri) -> {
          if (!name.matches("[a-zA-Z0-9-]+")
              || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
              || uri.getHost() == null
              || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))
              || uri.getRawQuery() != null
              || uri.getRawUserInfo() != null
              || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                "Spy provider must have a simple name and an HTTP origin: " + name);
          }
        });
  }

  public record Tracing(boolean enabled, int maxInvocations, int maxSpans) {
    public Tracing {
      if (maxInvocations < 1 || maxSpans < 1) {
        throw new IllegalArgumentException("Spy tracing limits must be positive");
      }
    }
  }
}
