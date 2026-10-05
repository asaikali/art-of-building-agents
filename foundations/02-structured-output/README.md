# 02 Structured Output

From the repository root, run the app with `OPENAI_API_KEY` set in your environment:

```shell
./mvnw -pl foundations/02-structured-output -am install -DskipTests
./mvnw -f foundations/02-structured-output/pom.xml spring-boot:run
```

Stop lesson 01 first if it is still using port 8080. Open
[Spy](http://localhost:8080/spy) to inspect the model requests and responses.

The samples in
[StructuredOutputController](src/main/java/com/example/foundations/structured/StructuredOutputController.java)
use Douglas Adams, the author of *The Hitchhiker's Guide to the Galaxy*.
Start with one book as text, return it as a record, then ask for all his books.
Finally, use a second record to get publication years from the same prompt.
All four samples are ready to run; no source edits or restarts are needed
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

## Takeaways

- `.content()` gives the application text; `.entity(...)` gives it typed data.
- The record defines the fields, and an array asks for a collection of records.
- A different record changes the schema while the user prompt stays the same.

Structured output defines the answer's shape. The facts and completeness of
the book list still depend on the model's knowledge.
