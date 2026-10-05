# Spy

Spy supplies the shared application configuration and embeds a Spring Cloud Gateway MVC proxy in each sample. Open [http://localhost:8080/spy](http://localhost:8080/spy) while running a sample to inspect the actual HTTP requests and responses sent between the app and the model.

The viewer shows each exchange separately, including prompts, tool definitions, tool results, response bodies, status, and duration. Exchanges run from oldest at the top to latest at the bottom. It follows the latest exchange automatically; select an earlier exchange to walk through it. Clear the history before starting another demo.

## Shared defaults

All common settings live in [spy-defaults.properties](src/main/resources/spy-defaults.properties):

| Setting | Default |
| --- | --- |
| Server port | `8080` |
| Gateway HTTP transport | JDK HTTP client |
| Chat provider | OpenAI |
| API key | `OPENAI_API_KEY` environment variable |
| Chat model | `gpt-5.4-mini` |
| Model base URL | `http://127.0.0.1:${server.port}/spy/proxy/openai/v1` |
| OpenAI upstream | `https://api.openai.com` |
| History | Latest 100 exchanges, up to 256 KiB per request or response body |

Each sample's `application.yml` only needs its application name and demo-specific settings. Spring Boot configuration in the application, environment, or command line overrides these defaults. For example, `SERVER_PORT=9090` moves the app, viewer, and loopback URL to port 9090.

Spy is a regular dependency with Spring Boot auto-configuration. Foundations samples inherit it through their group POM; meal-agent samples receive it through `agent-core`.

## How traffic flows

```text
ChatClient → OpenAI SDK → /spy/proxy/openai/v1/chat/completions
                       → Gateway MVC → https://api.openai.com/v1/chat/completions
```

Gateway removes `/spy/proxy/openai` and forwards the remaining path, query, headers, and body. Capture is part of the Gateway route:

```java
.before(uri(upstream))
.before(stripPrefix(3))
.before(capture::captureRequest)
.before(adaptCachedBody())
.after(capture::captureResponse)
.onError(Exception.class, capture::gatewayError)
```

The request filter uses Gateway's body cache to record the request; `adaptCachedBody()` makes that same body available for forwarding. The response filter observes Gateway's response stream as it is read, so streaming responses continue to flow. History records completion after Gateway finishes writing the response. Capture limits only truncate the viewer's stored copy. Credentials in authorization, API key, and cookie headers are redacted in the viewer and forwarded unchanged.

Gzip responses are decoded for display while their original compressed bytes are forwarded unchanged.

History is in memory and disappears when the app stops. The viewer contains the full prompts and model responses and is intended for the workshop.

`spy.providers` maps provider names to upstream HTTP origins. Adding a provider route requires no SDK-specific capture code; its Spring AI starter and model configuration must also be selected. The `/v1` prefix belongs in the OpenAI SDK base URL and is forwarded exactly once.

Model integration tests that start without a web server use the upstream URL directly, since they have no local gateway. An explicit `spring.ai.openai.base-url` also overrides loopback when needed.
