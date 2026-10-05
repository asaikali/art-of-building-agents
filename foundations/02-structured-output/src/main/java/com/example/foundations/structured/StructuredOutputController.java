package com.example.foundations.structured;

import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.core.convert.support.DefaultConversionService;
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

  // 1. The model knows the books; content() returns its answer as text.
  @GetMapping(path = "/books", produces = MediaType.TEXT_PLAIN_VALUE)
  public String books(@RequestParam(defaultValue = "Jane Austen") String author) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                        List the novels written by {author}.
                        Provide only the list, with no other commentary.
                        """)
                    .param("author", author))
        .call()
        .content();
  }

  // 2. A converter asks for comma-separated output and turns it into a Java list.
  @GetMapping(path = "/books/list", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> booksList(@RequestParam(defaultValue = "Jane Austen") String author) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                        List the novels written by {author}.
                        Provide only the list, with no other commentary.
                        """)
                    .param("author", author))
        .call()
        .entity(new ListOutputConverter(new DefaultConversionService()));
  }

  // 3. A converter asks for a JSON object and turns it into a Java map.
  @GetMapping(path = "/books/map", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> booksMap(@RequestParam(defaultValue = "Jane Austen") String author) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                        List the novels written by {author}.
                        Provide only the list, with no other commentary.
                        """)
                    .param("author", author))
        .call()
        .entity(new MapOutputConverter());
  }

  // 4. The record defines the schema. Add a field to Book during the live demo.
  @GetMapping(path = "/books/object", produces = MediaType.APPLICATION_JSON_VALUE)
  public Book[] booksObject(@RequestParam(defaultValue = "Jane Austen") String author) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                        List the novels written by {author}.
                        Provide only the list, with no other commentary.
                        """)
                    .param("author", author))
        .call()
        .entity(Book[].class);
  }
}
