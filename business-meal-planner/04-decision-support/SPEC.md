# 04 Decision Support Spec

## Purpose

After Phase 3 produces a restaurant shortlist, the user needs to explore the options before
deciding. This phase handles follow-up questions, comparisons, and selection — without
throwing the user back into requirement gathering.

## Problem

In Phase 3, the handler resets to `GATHERING_REQUIREMENTS` after planning. So when the user
asks "compare Canoe vs The Chase," the alignment pipeline treats it as a new meal request
instead of answering the question.

## New State

Add `EXPLORING_OPTIONS` to the workflow. After planning produces results, the status moves
to `EXPLORING_OPTIONS` instead of resetting to `GATHERING_REQUIREMENTS`.

Updated flow:

```
GATHERING → CONFIRMING → CONFIRMED → planning → EXPLORING_OPTIONS
```

While in `EXPLORING_OPTIONS`, user messages go to a decision support handler that can:

- answer questions about the shortlisted restaurants
- compare options side by side
- provide more detail on a specific restaurant
- help the user pick one

## Exit Conditions

From `EXPLORING_OPTIONS`, the user can:

- **Pick a restaurant** — acknowledge the choice (Phase 5 booking would handle the rest)
- **Change constraints** — reset to `GATHERING_REQUIREMENTS`, go through alignment again,
  then plan again with updated requirements
- **Ask for more options** — re-run planning (back to `CONFIRMED`)

The decision support model should detect which case applies from the user's message.

## Context Available to Decision Support

The decision support ChatClient should have access to:

- **Shortlist summary** in the system prompt — so it can answer comparison questions directly
  without tool calls ("which is quieter?", "compare X vs Y")
- **Tools** — the same planning tools (getRestaurantDetails, checkRestaurantCandidate) so
  it can look up deeper information when the user asks ("what's on the menu at Canoe?",
  "how far is Richmond Station from my office?")
- **Confirmed requirements** — so it knows what the user cares about

## System Prompt Strategy

The decision support system prompt should tell the model:

- You are helping the user explore restaurant recommendations
- The shortlist and confirmed requirements are provided below
- Answer questions, compare options, and help the user decide
- If the user wants to change requirements or try different constraints, say so and the
  system will restart the search
- If the user picks a restaurant, confirm the choice
- Write like a concierge — warm, helpful, specific
- Use tools to look up details when the user asks about something not in the shortlist

## What Needs to Change

### Agent context
- Store the planning result (shortlist text) in `JarvisAgentContext`
- Add the `EXPLORING_OPTIONS` status (or use a separate phase enum)

### Agent handler
- After planning: set status to `EXPLORING_OPTIONS` instead of `GATHERING_REQUIREMENTS`
- When `EXPLORING_OPTIONS`: route to a decision support handler
- When the model detects a constraint change: reset to `GATHERING_REQUIREMENTS`

### New class
- `DecisionSupportAdvisor` or similar — builds ChatClient with shortlist context + tools,
  handles user questions about recommendations

## Example Interactions

**User:** "Compare Canoe and The Chase"
**Agent:** Compares atmosphere, price range, vegetarian options, and makes a recommendation
based on the VIP dinner context.

**User:** "What's on the menu at Canoe?"
**Agent:** Uses `getRestaurantDetails` tool to look up menu details and summarizes
vegetarian options.

**User:** "Let's go with Canoe"
**Agent:** Confirms the choice.

**User:** "Actually, can we try with a $150 budget instead?"
**Agent:** Detects constraint change, resets to alignment, re-confirms with updated budget,
re-runs planning.

## Teaching Goal

Students learn how to keep an agent in a conversational exploration phase with access to
both cached context (the shortlist) and on-demand tools (restaurant details). The key
insight: not every user message needs to restart the full pipeline — sometimes the agent
just needs to answer a question about results it already has.
