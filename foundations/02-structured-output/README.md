# 02 Structured Output

After [01 ChatClient](../01-chat-client/README.md), use this lesson to learn the
typed model responses needed by [Agent 01: Intent Alignment](../../meal-agent/01-intent-alignment/README.md).

From the repository root, run the app with `OPENAI_API_KEY` set in your environment:

```shell
./mvnw -pl foundations/02-structured-output -am install -DskipTests
./mvnw -f foundations/02-structured-output/pom.xml spring-boot:run
```

Stop lesson 01 first if it is still using port 8080. Open
[Spy](http://localhost:8080/) to inspect the model requests and responses.

The samples in
[StructuredOutputController](src/main/java/com/example/foundations/structured/StructuredOutputController.java)
use Douglas Adams, the author of *The Hitchhiker's Guide to the Galaxy*.
Start with one book as text, return it as a record, then ask for all his books.
Use a second record to get publication years from the same prompt, then
compare the single-book request with native structured output and schema validation.
All six samples are ready to run; no source edits or restarts are needed
between them.

## 1. Ask for the first book as text

```java
@GetMapping(path = "/book/text", produces = MediaType.TEXT_PLAIN_VALUE)
public String firstBookText(@RequestParam(defaultValue = "Douglas Adams") String author) {
  return chatClient
      .prompt()
      .user(u -> u.text("What is the first book published by {author}?")
          .param("author", author))
      .call()
      .content();
}
```

```bash
http GET :8080/structured-output/book/text
```

**What to observe:** the model answers the question, but the application gets
a string. The title and author are not separate fields it can use.

## 2. Return the same book as a record

Define the shape of the answer in
[Book](src/main/java/com/example/foundations/structured/Book.java):

```java
public record Book(String author, String title) {}
```

Keep the question and finish with `.entity(Book.class)`:

```java
@GetMapping(path = "/book", produces = MediaType.APPLICATION_JSON_VALUE)
public Book firstBook(@RequestParam(defaultValue = "Douglas Adams") String author) {
  return chatClient
      .prompt()
      .user(u -> u.text("What is the first book published by {author}?")
          .param("author", author))
      .call()
      .entity(Book.class);
}
```

```bash
http GET :8080/structured-output/book
```

**What to observe:** the controller returns a `Book`, which Spring MVC
serializes as a JSON object with `author` and `title`. In Spy, compare this
request with the text sample: Spring AI adds a schema derived from the
record and converts the model's answer into that record.

## 3. Ask for all the books

Change the question to ask for all the author's books and return `Book[]`:

```java
@GetMapping(path = "/books", produces = MediaType.APPLICATION_JSON_VALUE)
public Book[] books(@RequestParam(defaultValue = "Douglas Adams") String author) {
  return chatClient
      .prompt()
      .user(u -> u.text("List all the books written by {author}.")
          .param("author", author))
      .call()
      .entity(Book[].class);
}
```

```bash
http GET :8080/structured-output/books
```

**What to observe:** the response is now a JSON array. Each entry has the
same fields as the single-book response. The Java return type describes
whether the application expects one book or a collection.

## 4. Get publication years with a second record

The second record adds one field:

```java
public record BookWithPublicationYear(String author, String title, Integer publicationYear) {}
```

Use that type with the exact same prompt as the previous sample:

```java
@GetMapping(path = "/books/with-year", produces = MediaType.APPLICATION_JSON_VALUE)
public BookWithPublicationYear[] booksWithYear(
    @RequestParam(defaultValue = "Douglas Adams") String author) {
  return chatClient
      .prompt()
      .user(u -> u.text("List all the books written by {author}.")
          .param("author", author))
      .call()
      .entity(BookWithPublicationYear[].class);
}
```

```bash
http GET :8080/structured-output/books/with-year
```

**What to observe:** the response now includes `publicationYear`, even though
the user prompt never asks for it. Compare the last two requests in Spy:
the question is identical, while the schema from
[BookWithPublicationYear](src/main/java/com/example/foundations/structured/BookWithPublicationYear.java)
includes the extra field. The model is asked to populate that field from
its knowledge.

For a quick factual check, *The Hitchhiker's Guide to the Galaxy* was
published in 1979. See [the publisher's page](https://www.panmacmillan.com/authors/douglas-adams/the-hitchhikers-guide-to-the-galaxy/9781509809066).

## 5. Return one book with native structured output

Keep the single-book question and record from sample 2. Enable the provider's
structured output support on this call:

```java
@GetMapping(path = "/book/native", produces = MediaType.APPLICATION_JSON_VALUE)
public Book firstBookNative(@RequestParam(defaultValue = "Douglas Adams") String author) {
  return chatClient
      .prompt()
      .user(u -> u.text("What is the first book published by {author}?")
          .param("author", author))
      .call()
      .entity(Book.class, spec -> spec.useProviderStructuredOutput());
}
```

Run the two single-book variants back to back:

```bash
http GET :8080/structured-output/book
http GET :8080/structured-output/book/native
```

**What to observe:** both endpoints return a `Book`. Spring AI still
generates the schema from the record in both cases. Compare the raw request
bodies in Spy:

| Request field | `/book` | `/book/native` |
| --- | --- | --- |
| `messages` | Question plus schema and JSON formatting instructions | Question without the added formatting instructions |
| `response_format` | Absent | `type: "json_schema"` with the schema in `json_schema.schema` |

The native call sends the schema as an OpenAI API parameter so the provider
enforces the response structure. Spring AI then deserializes the returned
JSON into the same `Book` record.

See [Spring AI's native structured output reference](https://docs.spring.io/spring-ai/reference/api/structured-output/native.html).

## 6. Validate the single-book response

Use the same question and record, this time enabling schema validation:

```java
@GetMapping(path = "/book/validated", produces = MediaType.APPLICATION_JSON_VALUE)
public Book firstBookValidated(@RequestParam(defaultValue = "Douglas Adams") String author) {
  return chatClient
      .prompt()
      .user(u -> u.text("What is the first book published by {author}?")
          .param("author", author))
      .call()
      .entity(Book.class, spec -> spec.validateSchema());
}
```

```bash
http GET :8080/structured-output/book/validated
```

**What to explain:** Spring AI checks the returned JSON against the `Book`
schema. If validation fails, it adds the specific errors to the user message
and asks the model to correct its answer, allowing three retries by default.
Validation checks the structure, not whether the book facts are correct.

A valid answer needs only one model call, so this sample may look just like
`/book` in Spy. If a response fails validation, Spy shows the extra calls and
the validation feedback in their prompts. This sample is ready to explain
without needing to provoke a model error during the talk.

See [Spring AI's schema validation reference](https://docs.spring.io/spring-ai/reference/api/structured-output/validation.html).

## Takeaways

- `.content()` gives the application text; `.entity(...)` gives it typed data.
- The record defines the fields, and an array asks for a collection of records.
- A different record changes the schema while the user prompt stays the same.
- Native structured output sends the schema through the provider API.
- Schema validation checks the response and retries with feedback when it fails.

Structured output defines the answer's shape. The facts and completeness of
the book list still depend on the model's knowledge.
