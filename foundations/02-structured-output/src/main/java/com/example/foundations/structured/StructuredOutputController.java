package com.example.foundations.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/structured-output")
public class StructuredOutputController {

  private final ChatClient chatClient;

  public StructuredOutputController(ChatClient.Builder builder) {
    this.chatClient = builder.build();
  }

  // 1. Ask for one book and get the answer as text.
  @GetMapping(path = "/book/text", produces = MediaType.TEXT_PLAIN_VALUE)
  public String firstBookText(@RequestParam(defaultValue = "Douglas Adams") String author) {
    return chatClient
        .prompt()
        .user(u -> u.text("What is the first book published by {author}?").param("author", author))
        .call()
        .content();
  }

  // 2. Keep the same question and ask Spring AI for a Book.
  @GetMapping(path = "/book", produces = MediaType.APPLICATION_JSON_VALUE)
  public Book firstBook(@RequestParam(defaultValue = "Douglas Adams") String author) {
    return chatClient
        .prompt()
        .user(u -> u.text("What is the first book published by {author}?").param("author", author))
        .call()
        .entity(Book.class);
  }

  // 3. Ask for all the books and return an array of the same record.
  @GetMapping(path = "/books", produces = MediaType.APPLICATION_JSON_VALUE)
  public Book[] books(@RequestParam(defaultValue = "Douglas Adams") String author) {
    return chatClient
        .prompt()
        .user(u -> u.text("List all the books written by {author}.").param("author", author))
        .call()
        .entity(Book[].class);
  }

  // 4. Keep the same prompt; the second record adds the publication year to the schema.
  @GetMapping(path = "/books/with-year", produces = MediaType.APPLICATION_JSON_VALUE)
  public BookWithPublicationYear[] booksWithYear(
      @RequestParam(defaultValue = "Douglas Adams") String author) {
    return chatClient
        .prompt()
        .user(u -> u.text("List all the books written by {author}.").param("author", author))
        .call()
        .entity(BookWithPublicationYear[].class);
  }

  // 5. Send the same book schema through the provider's API-level structured output support.
  @GetMapping(path = "/book/native", produces = MediaType.APPLICATION_JSON_VALUE)
  public Book firstBookNative(@RequestParam(defaultValue = "Douglas Adams") String author) {
    return chatClient
        .prompt()
        .user(u -> u.text("What is the first book published by {author}?").param("author", author))
        .call()
        .entity(Book.class, spec -> spec.useProviderStructuredOutput());
  }
}
