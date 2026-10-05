# 02 Restaurant Search

We have confirmed what the user wants. Now let the agent find available restaurants.

> **Builds on:** [01 Intent Alignment](../01-intent-alignment/README.md).
> **Before this lesson:** [Foundations 03: Tool Calling](../../foundations/03-tool-calling/README.md).
> **Adds:** one restaurant search tool and a model call that uses it after confirmation.

## The idea

In the foundations lesson, the model requested weather from a Java method. Here,
we use the same pattern to request restaurant availability. The application
supplies the confirmed requirements; the model chooses the tool arguments; Spring
AI executes the tool and returns its result to the model to write the reply.

```text
User message
  → existing alignment pipeline
  → requirements confirmed?
      no  → keep gathering or confirming
      yes → RestaurantSearcher.search(requirements)
              → model requests findAvailableRestaurants
              → Java searches the restaurant catalog
              → model presents available candidates
```

## Read these three changes

1. [RestaurantSearchTools](src/main/java/com/example/jarvis/search/RestaurantSearchTools.java)
   exposes `findAvailableRestaurants` with `@Tool` and `@ToolParam`. It accepts date,
   time, party size, and an optional neighborhood. Its JSON result contains only
   restaurant IDs, names, and neighborhoods.
2. [RestaurantSearcher](src/main/java/com/example/jarvis/search/RestaurantSearcher.java)
   makes one `ChatClient` call with `.tools(searchTools)`, just like the weather
   example. Spring AI handles the tool execution loop. The prompt asks for
   available candidates and says which requirements remain unchecked.
3. [JarvisAgentHandler](src/main/java/com/example/jarvis/agent/JarvisAgentHandler.java)
   triggers search only after alignment confirms the requirements. It publishes
   the reply, search state, and `search-started` / `search-completed` events to the
   inspector. It then returns to gathering so the user can change the request.

The requirements model and alignment pipeline are copied from 01 unchanged.
This is a standalone lesson with its own application and `pom.xml`.

## Run the demo

Set `OPENAI_API_KEY`. From the repository root:

```bash
./mvnw -pl meal-agent/02-restaurant-search -am install -DskipTests
./mvnw -f meal-agent/02-restaurant-search/pom.xml spring-boot:run
```

Start the inspector in another terminal:

```bash
cd scaffold/inspector
npm run dev
```

Open the inspector at <http://localhost:5173> and Spy at <http://localhost:8080/>.
Stop the previous lesson first; the backends use the same port.

Try this request, then confirm the captured requirements:

> Business dinner on October 20, 2026 at 6 pm for two people. Budget CAD 60 per
> person. One guest is vegetarian, and we want somewhere quiet.

The catalog uses simulated availability. **18:00 and party size 2** make all
restaurants available before any neighborhood filtering, so this example is
repeatable. **20:00** returns no available restaurants; use a new session with
that time to see the empty-result path.

In Spy, find the request containing the search tool, the model's `tool_calls`,
and the following request containing the `tool` result. In the inspector, watch
requirements confirmation followed by the search events and candidate reply.
Count tool calls separately from model HTTP exchanges.

## The problem this leaves us with

A restaurant may have a table and still be too expensive, too loud, unsuitable
for a vegetarian, or too far away. The tool result contains no evidence for
those judgments. The agent should present candidates and explain that those
requirements still need checking.

Inspect the search result in Spy: Canoe can be available, but nothing in these
three fields tells us whether CAD 60 is enough. That is the starting question
for [03 Constraint Checking](../03-constraint-checking/README.md).

## Tests

```bash
./mvnw -pl meal-agent/02-restaurant-search -am test
```

The tests use no model calls. They check availability and neighborhood filtering,
the limited result shape, the empty result, and the requirement that search starts
only after confirmation. The copied alignment tests continue to run as well.
