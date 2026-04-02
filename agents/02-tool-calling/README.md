# Step 02 — Tool Calling

The first real agent. Jarvis now has an identity (system prompt), a tool (`searchRestaurants`), and an agent loop (`ToolCallAdvisor`).

## What's new vs Step 01

- **System prompt** — Jarvis is a "business dinner planning assistant for Barcelona"
- **`@Tool` method** — `searchRestaurants(neighborhood)` returns hardcoded Barcelona restaurant data
- **`ToolCallAdvisor`** — Spring AI's built-in agent loop: `do { call LLM -> execute tool -> feed result back } while (hasToolCalls)`

## Key files

| File | Purpose |
|------|---------|
| `ToolCallingHandler.java` | System prompt + ChatClient with `ToolCallAdvisor.builder().build()` |
| `RestaurantTools.java` | `@Tool searchRestaurants` — 5 Barcelona restaurants with neighborhood, cuisine, price |

## Run it

```bash
cd agents/02-tool-calling
../../mvnw spring-boot:run
# Open http://localhost:8080
```

## What to try

- "Search for restaurants in the Gothic Quarter" — forces tool use, returns Can Culleretes
- "Find me a tapas place" — searches by cuisine, returns Cerveceria Catalana
- "What restaurants do you know about?" — may or may not use the tool

## What to observe

- The LLM decides **when** to call the tool — you don't code that logic
- `ToolCallAdvisor` handles the loop automatically: LLM -> tool call -> result -> LLM -> text response
- Check the **Events panel** to see tool call execution

## What's missing

No constraints. The agent will happily recommend a restaurant that costs more than your company allows. No turn limit — the loop runs until the LLM stops.

## Next step

[03-guardrails](../03-guardrails/) adds constraint-checking tools and a richer system prompt.
