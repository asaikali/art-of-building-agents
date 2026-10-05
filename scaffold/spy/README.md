# Spy

Spy supplies the shared application configuration and embeds a Spring Cloud Gateway MVC proxy in each sample. Open [http://localhost:8080/](http://localhost:8080/) while running a sample to inspect the actual HTTP requests and responses sent between the app and the model. `/spy` remains available as an alias. Meal-agent APIs stay under `/api`; the separate inspector runs on port 5173.

The navigator groups HTTP exchanges into expandable **turns**, from the initial prompt through tool requests and tool results to the final model response. Each exchange remains selectable, with prompts, tool definitions, tool results, response bodies, status, and duration. Turns and their exchanges run from oldest at the top to latest at the bottom. The viewer follows the latest exchange automatically; select an earlier exchange to walk through it. Clear the history before starting another demo.

Turn grouping matches OpenAI tool-call IDs returned by the model to the tool-result IDs in the following request, including streamed responses. Repeated prompts and interleaved requests stay separate. HTTP exchanges and tool calls are counted separately: two tools requested together can still mean only two HTTP exchanges. Tool execution itself happens in the app; Spy shows the messages exchanged with the model. Calls without matching IDs are shown separately, and a retained continuation whose earlier exchange is missing is labeled **Start not captured**. Arbitrary agent workflows with independent model calls would need an explicit turn ID for broader grouping.

Each body defaults to **Formatted**, the first view button. It shows readable content as an indented, syntax-highlighted code view with braces and brackets. JSON inside a string is expanded with a `JSON inside string` annotation; multiline prompts use triple quotes and real line breaks. This is a display representation.

Tool calls and their results share a background color, matched by tool-call ID across both panels and all exchanges in a turn. Each call gets its own color, including parallel calls. The six-color palette repeats after six pairs.

## Advisor flow

Select **Advisor flow** to see each invocation's effective advisor configuration and an execution sequence diagram. This includes advisors configured on the builder and on individual requests, their names, types, and order values. The diagram records actual entry and return events, including repeated calls inside a tool loop, nested ChatClients, and errors. A configured advisor that never executes is absent from the sequence. Solid arrows enter; dashed arrows return. Model return labels link to captured HTTP exchanges using the provider response ID; ambiguous or missing IDs remain unlinked.

Advisor boxes lead with the class name, such as `ChatModelCallAdvisor` or `TraceAdvisor`. A secondary **Name** label preserves Spring AI's advisor name (for example, `call`, `First`, or `Second`) so multiple instances of the same class remain distinguishable.

Spy registers a Micrometer observation handler for Spring AI's existing ChatClient, advisor, ChatModel and tool observations. It does not insert or wrap advisors, modify prompts, or change tool execution. Tool execution appears when its ToolCallingManager uses the application's ObservationRegistry. Only execution metadata and a short invocation prompt are retained; advisor contexts, HTTP headers, tool arguments/results and exception messages are not copied into traces. The HTTP view continues to show captured model traffic. The diagram shows invocation boundaries and message counts, rather than diffs of an advisor's prompt transformations.

Tracing is enabled by default and can be disabled with `spy.tracing.enabled=false`. History is bounded by `spy.tracing.max-invocations=100` and `spy.tracing.max-spans=300` per invocation. A truncated trace is labeled. **Clear** clears traffic and invocation history; **Pause** and **Follow latest** apply to both views. Recording failures are isolated from agent execution.

Restart a running sample after installing this version of Spy to register the observation handler. Calls made before tracing was enabled cannot acquire a sequence retrospectively. The Boot observation module supplies the registry without adding Actuator endpoints or requiring an external tracing service.

Use **Hide navigation** to give the diagram the full window width; **Show navigation** restores the invocation list. The control also works in Traffic view, and its state is remembered across reloads in the same browser tab.

**Parsed** provides an expandable tree. Click an object or array to expand or collapse it, or use **Expand all** and **Collapse all**. Prompt strings show real line breaks; JSON inside strings, including tool arguments, structured responses, and fenced schemas, is decoded and labeled so its original string type is clear. Polling preserves the branches you are exploring. **Raw** shows valid JSON with indentation and syntax highlighting while preserving the original string escaping and number values. Plain text and incomplete streaming bodies remain readable as text in every view.

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

The request filter uses Gateway's body cache to record the request; `adaptCachedBody()` makes that same body available for forwarding. The response filter observes Gateway's response stream as it is read, so streaming responses continue to flow. History records completion after Gateway finishes writing the response. Capture limits only truncate the viewer's stored copy. HTTP headers are forwarded unchanged but are never stored or exposed by Spy. Only a gzip flag is retained to decode compressed response bodies for display.

Gzip responses are decoded for display while their original compressed bytes are forwarded unchanged.

History is in memory and disappears when the app stops. The viewer contains the full prompts and model responses and is intended for the workshop.

`spy.providers` maps provider names to upstream HTTP origins. Adding a provider route requires no SDK-specific capture code; its Spring AI starter and model configuration must also be selected. The `/v1` prefix belongs in the OpenAI SDK base URL and is forwarded exactly once.

Model integration tests that start without a web server use the upstream URL directly, since they have no local gateway. An explicit `spring.ai.openai.base-url` also overrides loopback when needed.

## Checks

Run the Gateway integration tests and the turn-grouping tests from the repository root:

```shell
./mvnw -f scaffold/spy/pom.xml test
node --test scaffold/spy/src/test/javascript/*.test.mjs
```

The Java tests exercise the native SDK against local stub providers, including advisor order, repeated tool rounds, nested clients, streaming, short-circuiting, disabled tracing and bounded history. The JavaScript tests use Node's built-in test runner and cover turn grouping, pair colors, sequence order and response-ID links.
