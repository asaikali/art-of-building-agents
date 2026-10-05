package com.example.foundations.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = "/chat",
    consumes = MediaType.TEXT_PLAIN_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE)
public class ChatClientController {

  private final ChatClient chatClient;

  public ChatClientController(ChatClient.Builder builder) {
    this.chatClient = builder.build();
  }

  // 1. A user message goes in; the assistant's text comes out.
  @PostMapping("/basic")
  public String basic(@RequestBody String message) {
    return chatClient.prompt().user(message).call().content();
  }

  // 2. System instructions set the behavior; the user message supplies the request.
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

  // 3. A parameter substitutes a value into the prompt template.
  @PostMapping("/template")
  public String template(@RequestBody String topic) {
    return chatClient
        .prompt()
        .user(u -> u.text("Tell me a short joke about {topic}.").param("topic", topic))
        .call()
        .content();
  }
}
