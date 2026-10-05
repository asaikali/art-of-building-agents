package com.example.spy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/** Observes the gateway's servlet streams without buffering or rewriting the forwarded bodies. */
final class SpyCaptureFilter extends OncePerRequestFilter {
  private static final Set<String> SECRET_HEADERS =
      Set.of(
          "authorization", "proxy-authorization", "x-api-key", "api-key", "cookie", "set-cookie");
  private final SpyExchangeStore store;
  private final SpyProperties properties;

  SpyCaptureFilter(SpyExchangeStore store, SpyProperties properties) {
    this.store = store;
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    String rest = path.substring("/spy/proxy/".length());
    int separator = rest.indexOf('/');
    String provider = separator < 0 ? rest : rest.substring(0, separator);
    var upstream = properties.providers().get(provider);
    String destination =
        upstream == null ? path : upstream + (separator < 0 ? "/" : rest.substring(separator));
    if (request.getQueryString() != null) {
      destination += "?" + request.getQueryString();
    }
    var headers = new LinkedHashMap<String, List<String>>();
    Collections.list(request.getHeaderNames())
        .forEach(
            name -> headers.put(name, redact(name, Collections.list(request.getHeaders(name)))));
    var exchange = store.begin(request.getMethod(), destination, headers);
    String error = null;
    try {
      chain.doFilter(
          captureRequest(request, exchange.requestBody),
          captureResponse(response, exchange.responseBody));
    } catch (IOException | ServletException | RuntimeException exception) {
      error = exception.getClass().getSimpleName();
      throw exception;
    } finally {
      var responseHeaders = new LinkedHashMap<String, List<String>>();
      response
          .getHeaderNames()
          .forEach(
              name ->
                  responseHeaders.put(name, redact(name, List.copyOf(response.getHeaders(name)))));
      exchange.finish(response.getStatus(), responseHeaders, error);
    }
  }

  private static List<String> redact(String name, List<String> values) {
    return SECRET_HEADERS.contains(name.toLowerCase(Locale.ROOT))
        ? List.of("[redacted]")
        : List.copyOf(values);
  }

  private static HttpServletRequest captureRequest(
      HttpServletRequest request, SpyExchangeStore.BodyCapture capture) {
    return new HttpServletRequestWrapper(request) {
      private ServletInputStream stream;

      @Override
      public ServletInputStream getInputStream() throws IOException {
        if (stream == null) {
          var delegate = super.getInputStream();
          stream =
              new ServletInputStream() {
                @Override
                public int read() throws IOException {
                  int value = delegate.read();
                  if (value >= 0) capture.append(value);
                  return value;
                }

                @Override
                public int read(byte[] buffer, int offset, int length) throws IOException {
                  int count = delegate.read(buffer, offset, length);
                  if (count > 0) capture.append(buffer, offset, count);
                  return count;
                }

                @Override
                public boolean isFinished() {
                  return delegate.isFinished();
                }

                @Override
                public boolean isReady() {
                  return delegate.isReady();
                }

                @Override
                public void setReadListener(ReadListener listener) {
                  delegate.setReadListener(listener);
                }

                @Override
                public void close() throws IOException {
                  delegate.close();
                }
              };
        }
        return stream;
      }
    };
  }

  private static HttpServletResponse captureResponse(
      HttpServletResponse response, SpyExchangeStore.BodyCapture capture) {
    return new HttpServletResponseWrapper(response) {
      private ServletOutputStream stream;

      @Override
      public ServletOutputStream getOutputStream() throws IOException {
        if (stream == null) {
          var delegate = super.getOutputStream();
          stream =
              new ServletOutputStream() {
                @Override
                public void write(int value) throws IOException {
                  delegate.write(value);
                  capture.append(value);
                }

                @Override
                public void write(byte[] buffer, int offset, int length) throws IOException {
                  delegate.write(buffer, offset, length);
                  capture.append(buffer, offset, length);
                }

                @Override
                public boolean isReady() {
                  return delegate.isReady();
                }

                @Override
                public void setWriteListener(WriteListener listener) {
                  delegate.setWriteListener(listener);
                }

                @Override
                public void flush() throws IOException {
                  delegate.flush();
                }

                @Override
                public void close() throws IOException {
                  delegate.close();
                }
              };
        }
        return stream;
      }
    };
  }
}
