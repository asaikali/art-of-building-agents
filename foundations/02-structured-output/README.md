# 02 Structured Output

From the repository root, run the app with `OPENAI_API_KEY` set in your environment:

```shell
./mvnw -pl foundations/02-structured-output -am install -DskipTests
./mvnw -f foundations/02-structured-output/pom.xml spring-boot:run
```

Stop lesson 01 first if it is still using port 8080. Open
[Spy](http://localhost:8080/spy) to see the model requests and responses as you
run each command.

Walk through the samples below: each pairs a method from
[StructuredOutputController](src/main/java/com/example/foundations/structured/StructuredOutputController.java)
with an HTTPie command. All four ask the same question about novels the model
already knows. The return type changes from text to a list, a map, and typed
records.

## Creating the ChatClient

As in lesson 01, Spring Boot supplies a configured `ChatClient.Builder`:

```java
private final ChatClient chatClient;

public StructuredOutputController(ChatClient.Builder builder) {
  this.chatClient = builder.build();
}
```

## 1. Get text back

Ask for Jane Austen's novels and finish with `.content()`:

```java
@GetMapping(path = "/books", produces = MediaType.TEXT_PLAIN_VALUE)
public String books(@RequestParam(defaultValue = "Jane Austen") String author) {
  return chatClient
      .prompt()
      .user(
          u -> u.text("""
              List the novels written by {author}.
              Provide only the list, with no other commentary.
              """)
              .param("author", author))
      .call()
      .content();
}
```

```bash
http GET :8080/structured-output/books
```

**What to observe:** the answer is a string. The model chooses how to present
the list, such as numbered lines or bullet points. The application has no
individual book objects to work with.

## 2. Convert the answer to a list

Keep the prompt and replace `.content()` with a `ListOutputConverter`:

```java
@GetMapping(path = "/books/list", produces = MediaType.APPLICATION_JSON_VALUE)
public List<String> booksList(@RequestParam(defaultValue = "Jane Austen") String author) {
  return chatClient
      .prompt()
      .user(
          u -> u.text("""
              List the novels written by {author}.
              Provide only the list, with no other commentary.
              """)
              .param("author", author))
      .call()
      .entity(new ListOutputConverter(new DefaultConversionService()));
}
```

```bash
http GET :8080/structured-output/books/list
```

**What to observe:** the controller returns a `List<String>` and Spring MVC
serializes it as a JSON array. In Spy, look for the converter's instructions
requesting comma-separated values. The model's response is converted to a
Java list before the controller returns it.

## 3. Convert the answer to a map

Use a `MapOutputConverter` to get a JSON object as a Java map:

```java
@GetMapping(path = "/books/map", produces = MediaType.APPLICATION_JSON_VALUE)
public Map<String, Object> booksMap(@RequestParam(defaultValue = "Jane Austen") String author) {
  return chatClient
      .prompt()
      .user(
          u -> u.text("""
              List the novels written by {author}.
              Provide only the list, with no other commentary.
              """)
              .param("author", author))
      .call()
      .entity(new MapOutputConverter());
}
```

```bash
http GET :8080/structured-output/books/map
```

**What to observe:** the response is a JSON object. The converter requests
JSON, but the model still chooses the keys and values. The application gets
a `Map<String, Object>` without a domain type defining its fields.

## 4. Let a record define the output

Define the fields the application needs in
[Book](src/main/java/com/example/foundations/structured/Book.java):

```java
public record Book(String author, String title) {}
```

Pass `Book[].class` to `.entity(...)`:

```java
@GetMapping(path = "/books/object", produces = MediaType.APPLICATION_JSON_VALUE)
public Book[] booksObject(@RequestParam(defaultValue = "Jane Austen") String author) {
  return chatClient
      .prompt()
      .user(
          u -> u.text("""
              List the novels written by {author}.
              Provide only the list, with no other commentary.
              """)
              .param("author", author))
      .call()
      .entity(Book[].class);
}
```

```bash
http GET :8080/structured-output/books/object
```

**What to observe:** each object has `author` and `title` fields. Spring AI
derives a JSON schema from the record, includes formatting instructions in
the model request, and converts the answer to a `Book[]`. Inspect the request
in Spy to see the generated schema.

Change only the author to reuse the same prompt and record:

```bash
http GET :8080/structured-output/books/object author=='George Orwell'
```

## 5. Live demo: add a field to the record

The checked-in record contains only `author` and `title`. During the talk,
change it to:

```java
public record Book(String author, String title, Integer publicationYear) {}
```

Restart the app to compile and load the changed record. Leave the controller
and prompt as they are, then repeat the exact same request:

```bash
http GET :8080/structured-output/books/object
```

**What to observe:** the generated schema now includes `publicationYear`.
The model sees that field in the schema and is asked to fill it using its
knowledge, even though the user prompt still just asks for novels. Each
returned `Book` now has a publication year. Compare the requests in Spy
before and after the edit to connect the record change to the new schema.

Restore the two-field record when preparing to give the demo again.

## Takeaways

- `.content()` returns the model's answer as text.
- Output converters supply format instructions and parse the model's answer.
- `.entity(Book[].class)` uses the record to define the output structure.
- Adding a record field changes the generated schema without changing the prompt.
- Structured output defines the shape of the answer; factual accuracy still
  depends on the model's knowledge.

See the Spring AI reference for
[structured output](https://docs.spring.io/spring-ai/reference/api/structured-output.html)
and [output converters](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html).
