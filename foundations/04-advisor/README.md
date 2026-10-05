# 04 Advisors

Run this after [03 Tool Calling](../03-tool-calling/README.md). Keep the
weather and activity tools familiar and focus on what surrounds the model
calls.

From the repository root, with `OPENAI_API_KEY` set:

```shell
./mvnw -pl foundations/04-advisor -am install -DskipTests
./mvnw -f foundations/04-advisor/pom.xml spring-boot:run
```

Stop any other sample using port 8080. Open [Spy](http://localhost:8080/)
and keep the application console visible. Spy shows HTTP exchanges; the
console shows the advisor chain around them.

## An advisor surrounds the next step

An advisor can inspect or modify a request before passing it on, and inspect
or modify the response on its way back. Advisors compose into a chain.
Logging, memory, retrieval, and tool execution are examples of behaviors
that can live in this chain.

Our [TraceAdvisor](src/main/java/com/example/foundations/advisors/TraceAdvisor.java)
implements `CallAdvisor` for the blocking `.call()` examples:

```java
public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
  logger.info("[{}] before: {} messages", name, request.prompt().getInstructions().size());
  ChatClientResponse response = chain.nextCall(request);
  logger.info(
      "[{}] after: {}",
      name,
      response.chatResponse() != null && response.chatResponse().hasToolCalls()
          ? "tool calls"
          : "final answer");
  return response;
}
```

The key line is `chain.nextCall(request)`: it runs the remaining advisors
and eventually the model. The code before and after it surrounds that work.
This advisor only observes; it returns the response unchanged.

## 1. See the order and the return path

```bash
http GET :8080/advisors/order
```

The controller deliberately lists the advisors in reverse order:

```java
.advisors(new TraceAdvisor("Second", 1), new TraceAdvisor("First", 0))
.user("Why does it rain? Answer in one sentence.")
```

`getOrder()` controls execution: lower values run first on the request.
The response returns through the chain in reverse:

```text
[First] before: 1 messages
[Second] before: 1 messages
[Second] after: final answer
[First] after: final answer
```

Spy shows one model HTTP exchange. Two advisors do not mean two model calls.
These advisors apply to this request through `.advisors(...)`.

## 2. Observe the tool loop

```bash
http GET :8080/advisors/weather
```

In lesson 03, Spring AI's `ChatClient` already auto-registered
`ToolCallingAdvisor`. That advisor requests model responses, executes the
tools the model asks for, and sends their results back until the model
returns an answer.

Here we register it explicitly so its place in the chain is visible:

```java
builder.clone()
    .defaultAdvisors(
        new TraceAdvisor("Turn", ToolCallingAdvisor.DEFAULT_ORDER - 1),
        ToolCallingAdvisor.builder().toolCallingManager(toolCallingManager).build(),
        new TraceAdvisor("Model", ToolCallingAdvisor.DEFAULT_ORDER + 1))
    .build();
```

`defaultAdvisors(...)` applies this chain to every request through this
client. Spring AI uses the explicitly registered tool advisor in place of
the automatic one.

The orders put `Turn` before the tool advisor and `Model` after it:

```text
Turn → ToolCallingAdvisor → Model → chat model
```

The expected console sequence for one weather lookup is:

```text
[Turn] before: 1 messages
[Model] before: 1 messages
[Model] after: tool calls
[Model] before: 3 messages
[Model] after: final answer
[Turn] after: final answer
```

`Turn` observes the whole turn once. `Model` observes each model call,
including the intermediate response requesting a tool. The second request
adds an assistant tool-call message and a tool-result message.

Match the two `Model` pairs to the two HTTP exchanges in Spy. The controller
still contains one `.call()`.

## 3. The same chain, another iteration

```bash
http GET :8080/advisors/activities
```

This reuses lesson 03's dependent tools: get the weather, then find
activities using the returned condition and temperature. Both are supplied
with `.tools(weatherService, activityService)`.

The expected console sequence is:

```text
[Turn] before: 1 messages
[Model] before: 1 messages
[Model] after: tool calls
[Model] before: 3 messages
[Model] after: tool calls
[Model] before: 5 messages
[Model] after: final answer
[Turn] after: final answer
```

Spy should show weather request, activity request, and final recommendation
as three model HTTP exchanges. Compare the activity arguments with the
weather tool result. The model controls the tool calls, so the exact
exchange count and message counts can vary.

For a quick live change, move `Model` to `ToolCallingAdvisor.DEFAULT_ORDER - 2`
and restart. It now sits outside the loop and runs once per turn. Spy still
shows the model exchanges; the advisor's position changes what it observes.

See Spring AI's [advisor reference](https://docs.spring.io/spring-ai/reference/api/advisors.html)
and [ToolCallingAdvisor reference](https://docs.spring.io/spring-ai/reference/api/tools/tool-calling-advisor.html).
