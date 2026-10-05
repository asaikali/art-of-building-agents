package com.example.spy;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

/** Capture is attached to Gateway routes through their before and after filter functions. */
final class SpyGatewayFilters {
  private static final String EXCHANGE_ATTRIBUTE = SpyGatewayFilters.class.getName() + ".exchange";
  private static final byte[] GATEWAY_ERROR_BODY =
      "{\"error\":\"The model provider request failed.\"}".getBytes(StandardCharsets.UTF_8);
  private static final Set<String> SECRET_HEADERS =
      Set.of(
          "authorization", "proxy-authorization", "x-api-key", "api-key", "cookie", "set-cookie");
  private final SpyExchangeStore store;

  SpyGatewayFilters(SpyExchangeStore store) {
    this.store = store;
  }

  ServerRequest captureRequest(ServerRequest request) {
    URI upstream = MvcUtils.getAttribute(request, MvcUtils.GATEWAY_REQUEST_URL_ATTR);
    URI destination =
        UriComponentsBuilder.fromUri(request.uri())
            .scheme(upstream.getScheme())
            .host(upstream.getHost())
            .port(upstream.getPort())
            .build(true)
            .toUri();
    var body = MvcUtils.cacheBody(request);
    var exchange =
        store.begin(
            request.method().name(),
            destination.toString(),
            redactHeaders(request.headers().asHttpHeaders()));
    MvcUtils.putAttribute(request, EXCHANGE_ATTRIBUTE, exchange);
    try {
      body.transferTo(exchange.requestBody);
      body.reset();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return request;
  }

  ServerResponse captureResponse(ServerRequest request, ServerResponse response) {
    SpyExchangeStore.Exchange exchange = MvcUtils.getAttribute(request, EXCHANGE_ATTRIBUTE);
    exchange.responseStarted(response.statusCode().value(), redactHeaders(response.headers()));
    InputStream body = MvcUtils.getAttribute(request, MvcUtils.CLIENT_RESPONSE_INPUT_STREAM_ATTR);
    if (body != null) {
      // Gateway reads this stream when it writes the response, including each SSE chunk.
      MvcUtils.putAttribute(
          request,
          MvcUtils.CLIENT_RESPONSE_INPUT_STREAM_ATTR,
          new CapturingInputStream(body, exchange.responseBody));
    }
    return new CapturedResponse(response, exchange);
  }

  ServerResponse gatewayError(Throwable exception, ServerRequest request) {
    SpyExchangeStore.Exchange exchange = MvcUtils.getAttribute(request, EXCHANGE_ATTRIBUTE);
    if (exchange != null) {
      exchange.responseBody.write(GATEWAY_ERROR_BODY, 0, GATEWAY_ERROR_BODY.length);
    }
    return ServerResponse.status(HttpStatus.BAD_GATEWAY)
        .contentType(MediaType.APPLICATION_JSON)
        .body(GATEWAY_ERROR_BODY);
  }

  private static Map<String, List<String>> redactHeaders(HttpHeaders headers) {
    var captured = new LinkedHashMap<String, List<String>>();
    headers.forEach(
        (name, values) ->
            captured.put(
                name,
                SECRET_HEADERS.contains(name.toLowerCase(Locale.ROOT))
                    ? List.of("[redacted]")
                    : List.copyOf(values)));
    return captured;
  }

  private static final class CapturingInputStream extends FilterInputStream {
    private final SpyExchangeStore.BodyCapture capture;

    CapturingInputStream(InputStream input, SpyExchangeStore.BodyCapture capture) {
      super(input);
      this.capture = capture;
    }

    @Override
    public int read() throws IOException {
      int value = in.read();
      if (value >= 0) capture.write(value);
      return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int count = in.read(buffer, offset, length);
      if (count > 0) capture.write(buffer, offset, count);
      return count;
    }
  }

  /** Gateway's after filter runs before body writing; mark completion after writeTo returns. */
  private record CapturedResponse(ServerResponse delegate, SpyExchangeStore.Exchange exchange)
      implements ServerResponse {
    @Override
    public HttpStatusCode statusCode() {
      return delegate.statusCode();
    }

    @Override
    public HttpHeaders headers() {
      return delegate.headers();
    }

    @Override
    public MultiValueMap<String, Cookie> cookies() {
      return delegate.cookies();
    }

    @Override
    public ModelAndView writeTo(
        HttpServletRequest request, HttpServletResponse response, Context context)
        throws ServletException, IOException {
      String error = null;
      try {
        return delegate.writeTo(request, response, context);
      } catch (IOException | ServletException | RuntimeException exception) {
        error = exception.getClass().getSimpleName();
        throw exception;
      } finally {
        var headers = new HttpHeaders();
        response
            .getHeaderNames()
            .forEach(name -> headers.put(name, List.copyOf(response.getHeaders(name))));
        exchange.finish(response.getStatus(), redactHeaders(headers), error);
      }
    }
  }
}
