package com.example.spy;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.BodyFilterFunctions.adaptCachedBody;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.cloud.gateway.server.mvc.GatewayServerMvcAutoConfiguration;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.handler.ProxyExchange;
import org.springframework.cloud.gateway.server.mvc.handler.RestClientProxyExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@AutoConfiguration(before = GatewayServerMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(SpyProperties.class)
public class SpyAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean(ProxyExchange.class)
  ProxyExchange spyGatewayProxyExchange(
      RestClient.Builder client,
      ClientHttpRequestFactoryBuilder<?> factories,
      HttpClientSettings settings,
      GatewayMvcProperties properties) {
    var factory = factories.build(settings);
    if (factory instanceof JdkClientHttpRequestFactory jdk) {
      // Keep upstream bytes and Content-Encoding intact; only the viewer decodes them.
      jdk.enableCompression(false);
    }
    return new RestClientProxyExchange(client.clone().requestFactory(factory).build(), properties);
  }

  @Bean
  SpyExchangeStore spyExchangeStore(SpyProperties properties) {
    return new SpyExchangeStore(properties.maxExchanges(), properties.maxBodyBytes());
  }

  @Bean
  RouterFunction<ServerResponse> spyProviderRoutes(
      SpyProperties properties, SpyExchangeStore store) {
    var routes = RouterFunctions.route();
    var capture = new SpyGatewayFilters(store);
    properties
        .providers()
        .forEach(
            (name, upstream) ->
                routes.add(
                    route("spy-" + name)
                        .route(path("/spy/proxy/" + name + "/**"), http())
                        .before(uri(upstream))
                        .before(stripPrefix(3))
                        .before(capture::captureRequest)
                        .before(adaptCachedBody())
                        .after(capture::captureResponse)
                        .onError(Exception.class, capture::gatewayError)
                        .build()));
    return routes.build();
  }

  @Bean
  SpyController spyController(SpyExchangeStore store) {
    return new SpyController(store);
  }
}
