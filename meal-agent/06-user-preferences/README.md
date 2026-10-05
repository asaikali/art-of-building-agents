# 06 User Preferences

Builds on 05 decision support and applies [Foundations 04: Advisors](../../foundations/04-advisor/README.md).
The agent remembers favourite restaurants across chats for the same user.

## What this module teaches

- A session owns current meal requirements and a shortlist; a user owns preferences across sessions.
- `UserPreferenceAdvisor` loads preferences before planning and decision support, once per turn.
- The advisor runs at `ToolCallingAdvisor.DEFAULT_ORDER - 1`, outside the tool loop.
- Remember/forget tools write memory only on explicit user requests during decision support.
- User identity comes from `session.userId()`, never from a model-supplied tool argument.
- Per-call tool context carries identity and requirements without mutable fields on shared tool services.

The handler passes the session's user ID to the advisor context and tool context.
The advisor augments the current user message without replacing the client's system instructions.
Planning considers available favourites before choosing up to five candidates to evaluate.
Favourites must satisfy the same hard constraints; current requests such as "somewhere different"
take precedence. Recommendation ordering is model-driven, not a deterministic ranking guarantee.

`UserPreferenceStore` is a thread-safe in-memory store, initially empty. Preferences survive
New Chat, but reset when the backend restarts. The Inspector state shows the user's stored
preferences separately from their current meal requirements. Spy shows the enriched model prompt;
the application console logs `preferences-loaded` once per planning or decision-support turn.

## Run

```bash
mise run 6:start
# In another terminal:
mise run ui:start
```

Or build and run directly:

```bash
./mvnw -pl meal-agent/06-user-preferences -am install -DskipTests
./mvnw -f meal-agent/06-user-preferences/pom.xml spring-boot:run
```

## Demo walkthrough

1. Create a chat as **Alex**. Align and confirm requirements for a small client dinner,
   with a budget broad enough to include the restaurant you want to remember.
2. After the shortlist appears, say **"Remember Canoe as a favourite for client dinners."**
   The tool accepts its catalog ID or a uniquely matching restaurant name. Watch Spy and the preferences state.
   You can remember any restaurant in the shared catalog, even if it is not shortlisted.
3. Create another chat as **Alex**, confirm a new meal, and inspect the enriched planning prompt.
   Canoe should be considered if available, and prioritised if it meets the constraints.
4. Create a chat as **Ben**. His preferences are empty; Alex's memory is not injected.
5. In Alex's decision-support phase, ask **"Forget Canoe as a favourite."** Verify the stored list clears.
6. To demonstrate a current instruction overriding memory, start an Alex chat and say
   **"Plan a client dinner somewhere different from Canoe this time"** while specifying the meal.
   The current intent remains in the confirmed meal context and takes priority over the favourite.

Memory updates are deliberately introduced in the exploring-options phase. Alignment stays
focused on current meal requirements. A booking or positive comment does not automatically
create a favourite. Preferences do not automatically rerun an existing shortlist.

## Checks

```bash
./mvnw -pl meal-agent/06-user-preferences -am test
```

The preference tests require no API key. They check user isolation, explicit remember/forget tools,
unknown restaurant handling, advisor prompt enrichment, and omission of identity from tool schemas.
Live model behaviour requires `OPENAI_API_KEY` and the walkthrough above.

Browser verification exercised remembering Canoe by name, recalling it in a fresh Alex chat,
prioritising it in the shortlist, keeping Ben's preferences empty, and forgetting it.
For simultaneous Inspector tabs, use separate browsers or different local origins
(`localhost:5173` and `127.0.0.1:5173`) to avoid the HTTP/1 connection limit from SSE streams.
