# 01 ChatClient

From the repository root, run the app:

```shell
./mvnw -pl foundations/01-chat-client -am install -DskipTests
./mvnw -f foundations/01-chat-client/pom.xml spring-boot:run
```

Open [Spy](http://localhost:8080/) to see the model requests and responses as you run each command.

Walk through the samples below: each pairs a method from
[ChatClientController](src/main/java/com/example/foundations/chat/ChatClientController.java)
with an HTTPie command. They introduce user prompts, system prompts, and
template substitution.

## Creating the ChatClient

Spring Boot supplies a `ChatClient.Builder` configured for OpenAI. The
controller builds a client once and uses it in each sample:

```java
private final ChatClient chatClient;

public ChatClientController(ChatClient.Builder builder) {
  this.chatClient = builder.build();
}
```

## 1. Send a user prompt

### Ask a question and get text back

Send a question with `.user(message)` and get the assistant's text with
`.call().content()`. This call supplies no system instructions.

```java
@PostMapping("/basic")
public String basic(@RequestBody String message) {
  return chatClient
      .prompt()
      .user(message)
      .call()
      .content();
}
```

```bash
http POST :8080/chat/basic Content-Type:text/plain \
  --raw 'What is the color of a carrot?'
```

**What to observe:** the HTTP response body contains the assistant's answer.
Trace the calls: `.prompt()` starts a request, `.user(...)` supplies the
question, `.call()` invokes the model, and `.content()` gets the answer text.

## 2. Guide behavior with a system prompt

### Ask about vegetables

Ask the same question, but add a system prompt instructing the assistant to
discuss fruits only. Compare the response with the basic call: the assistant
is now asked to decline vegetable questions.

```java
@PostMapping("/system")
public String system(@RequestBody String message) {
  return chatClient
      .prompt()
      .system(
          """
          You are a helpful expert on plants.
          You only answer questions about fruits.
          If asked about vegetables, politely explain that you only discuss fruits.
          """)
      .user(message)
      .call()
      .content();
}
```

```bash
http POST :8080/chat/system Content-Type:text/plain \
  --raw 'What is the color of a carrot?'
```

**What to observe:** the user question is identical to the first sample.
Notice whether the assistant follows the fruits-only instruction and explains
its scope. Adding the system message is what changes the request to the model.

### Ask about fruit

Keep the same fruits-only system prompt and ask about a banana. This question
fits the instructions. The system prompt sets the behavior; the user prompt
supplies the question.

```java
@PostMapping("/system")
public String system(@RequestBody String message) {
  return chatClient
      .prompt()
      .system(
          """
          You are a helpful expert on plants.
          You only answer questions about fruits.
          If asked about vegetables, politely explain that you only discuss fruits.
          """)
      .user(message)
      .call()
      .content();
}
```

```bash
http POST :8080/chat/system Content-Type:text/plain \
  --raw 'What is the color of a banana?'
```

**What to observe:** the banana question fits the assistant's instructions.
Both calls use the same system prompt; only the user question changes.

## 3. Reuse a prompt with template parameters

Send just a topic. The controller uses `.text(...)` and `.param("topic", topic)`
to fill the placeholder in `Tell me a short joke about {topic}.`

```java
@PostMapping("/template")
public String template(@RequestBody String topic) {
  return chatClient
      .prompt()
      .user(
          u -> u.text("Tell me a short joke about {topic}.")
              .param("topic", topic))
      .call()
      .content();
}
```

First, use `cows` as the topic:

```bash
http POST :8080/chat/template Content-Type:text/plain \
  --raw 'cows'
```

Then change only the topic to `computers`:

```bash
http POST :8080/chat/template Content-Type:text/plain \
  --raw 'computers'
```

**What to observe:** each request body contains only a topic. The application
fills `{topic}` before sending the prompt to the model. Compare the subjects
of the two jokes: the template and controller method stay the same while the
parameter changes.

## Takeaways

- The **user message** supplies the request.
- The **system message** supplies instructions for the assistant's behavior.
- **Template parameters** fill placeholders before the prompt reaches the model.
