# 01 ChatClient

Three small demos introduce user prompts, system prompts, and template
substitution with Spring AI's `ChatClient`. Each controller method makes one
OpenAI call and returns the assistant's text.

## Run

Set `OPENAI_API_KEY` in your shell or IntelliJ run configuration. From the
repository root:

```bash
./mvnw -pl foundations/01-chat-client -am package -DskipTests
java -jar foundations/01-chat-client/target/01-chat-client-0.0.1-SNAPSHOT.jar
```

You can also run `ChatClientApplication` directly in IntelliJ. The app uses
`gpt-5.4-mini` and listens on port 8080. Stop any other app using that port
before starting this app, or use `--server.port=8081` with
the JAR and update `baseUrl` in `demos.http`.

## Demo sequence

Open `ChatClientController.java` beside `demos.http` in IntelliJ. Run the
requests individually and compare their responses. Model wording will vary.

1. **Basic call:** `.prompt().user(message).call().content()` sends a user
   message and returns text. `ChatClient.Builder` is supplied by Spring Boot
   auto-configuration using the OpenAI starter. Ask about a carrot's color
   without supplying any system instructions.
2. **System instructions:** `.system(...)` supplies the assistant's role and
   response instructions separately from the user's request. The assistant
   is instructed to discuss fruits only. Repeat the carrot question, then
   ask about a banana. Compare how the instructions affect the responses.
3. **Prompt parameters:** `.text("Tell me a short joke about {topic}.")` and
   `.param("topic", topic)` substitute a value into a reusable prompt. The
   request body supplies only the topic: first `cows`, then `computers`.
   Compare the responses while the template in the controller stays the same.

Each demo returns free-form text. Next, `02-structured-output` will show how
to turn a response into a typed Java object.
