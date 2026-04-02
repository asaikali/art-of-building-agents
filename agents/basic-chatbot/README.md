# Step 01 — Basic Chatbot

A bare `ChatClient` with no system prompt and no tools. This is the starting point: whatever the user types goes straight to the LLM.

## What's here

| File | Purpose |
|------|---------|
| `BasicChatbotHandler.java` | Implements `AgentHandler`, calls `chatClient.prompt().messages(history).call().content()` |
| `BasicChatbotApplication.java` | Spring Boot main class |

## Run it

```bash
cd agents/basic-chatbot
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to observe

- **Chat panel**: Type anything — the LLM responds directly
- **State panel**: Shows turn count and status
- **Events panel**: `user-message-received` and `assistant-reply-sent` events

## What's missing

Everything. No system prompt, no tools, no agent loop, no guardrails. The LLM has no identity and no capabilities beyond its training data.

## Next step

[02-tool-calling](../02-tool-calling/) adds a system prompt, a tool, and the agent loop.
