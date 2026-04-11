# Business Meal Planner Specification

## Purpose

Build an end-to-end business meal planning agent from first principles using Spring AI,
`ChatClient`, and tool calling.

The agent is for business meals broadly, including:

- customer dinners
- client lunches
- internal team meals
- hosted business meals where venue choice matters

The design should be teachable. Each phase adds one new architectural idea rather than
introducing the full system all at once.

## Planned Maven Module Structure

The `business-meal-planner` directory is intended to evolve from a single scaffolded app
into a parent Maven module that contains one child module per teaching phase.

Planned child modules:

- `01-intent-alignment`
- `02-constraint-checking`
- `03-restaurant-planning`
- `04-decision-support`
- `05-booking`

Planned structure:

```text
business-meal-planner/
  pom.xml
  README.md
  SPEC.md
  01-intent-alignment/
  02-constraint-checking/
  03-restaurant-planning/
  04-decision-support/
  05-booking/
```

Each child module should be a standalone sample app that demonstrates only the
architectural concept for that phase. The teaching goal is that students can run each
phase independently and see exactly what new capability was added.

## Core Product Goal

Given a messy natural-language request, the agent should:

1. align with the user on what they actually want
2. translate that into a set of constraints
3. determine how those constraints can be checked
4. search for and evaluate restaurant candidates
5. present a ranked shortlist
6. help the user compare and select an option
7. book the selected restaurant when requested

## Example User Prompt

> I have a business dinner tonight at 6 pm with 4 people, one is vegetarian, I am at the
> office until 5:30, book a restaurant.

This kind of prompt is intentionally messy and incomplete. The system should be designed
to handle that as the normal case.

## Constraint Taxonomy

The planner should always reason over these categories:

- meal context
  - lunch, dinner
  - customer dinner, client lunch, internal team meal
- action intent
  - recommend, shortlist, book
- attendee details
  - party size
  - client or VIP presence
- schedule
  - date and time
  - departure time
  - booking deadline
- travel
  - origin
  - travel mode
  - max travel time
  - max distance
- budget and policy
  - per-person budget
  - expense policy
  - spend sensitivity
- dietary and accessibility
  - vegetarian, vegan, allergies
  - accessibility needs
- venue suitability
  - noise level
  - privacy
  - professionalism
  - formality
- preferences
  - cuisine
  - neighborhood
  - prestige
  - atmosphere
- flexibility
  - hard constraints
  - soft preferences

## Constraint Classes

Every planning pass should distinguish between:

- explicit constraints
  - directly stated by the user
- inferred constraints
  - reasonable defaults for a business meal
- missing information
  - facts required before planning or booking can proceed
- assumptions
  - tentative interpretations that should be surfaced to the user

## Phase 1: Intent Alignment

Planned module: `01-intent-alignment`

### Goal

Convert a messy natural-language request into a confirmed set of business-meal
constraints before any restaurant search or booking happens.

### Responsibilities

- capture the shared meal information
- capture attendee-specific requirements
- identify missing information needed to continue
- determine whether the request should move to clarification or confirmation
- summarize understanding back to the user
- ask for confirmation or correction
- repeat until requirements are confirmed or clarification is needed

### Non-goals

- searching for restaurants
- validating restaurants
- ranking candidates
- booking

### Output

Phase 1 uses a Markdown string as the inspector artifact, but the underlying captured input is now
structured as:

- `UserRequirements`
  - `Meal`
  - `List<Attendee>`

Phase 1 workflow state is kept separately in agent context:

- `JarvisAgentContext`
  - `UserRequirements`
  - `missingInformation`
  - `RequirementStatus`

The Markdown must contain these sections:

```md
## Meal
## Additional Requirements
## Cuisine Preferences
## Attendees
## Missing Information
## Status
```

The `Status` section must end with one of:

- `Waiting for confirmation`
- `Waiting for clarification`
- `Requirements confirmed`

This phase should not store inferred defaults, assumptions, validation output, or search/planning
results inside the captured requirements model.

### Greeting And Session-Start Behavior

The agent should not start with a blank chat.

Each sample agent should be able to publish an initial assistant message when a session is
created so the UI can immediately show:

- what the agent does
- what kind of input it expects
- what the user should provide first

For the business meal planner, the initial message should introduce Jarvis and explain that
the planner first aligns on the meal requirements before searching or booking.

If the user sends only a greeting or opener such as:

- hi
- hello
- can you help

then the agent should respond with a short orientation message instead of a generic
clarification request.

### Required Search-Ready Constraints

Phase 1 should distinguish between:

- requirements captured
- requirements confirmed
- requirements ready for restaurant search

For this sample, the minimum required constraints to leave intent alignment are:

- when
  - date and time
- where from
  - origin address
- how many
  - party size

The planner may confirm other constraints before these are complete, but it should not be
considered search-ready until all three are present.

For this teaching sample, `where` means a concrete origin address, not only a neighborhood.

### Open Product Decision: Origin As A Required Phase 1 Field

This part of the specification is currently incomplete and must be confirmed before future
sessions change the implementation.

Current code behavior in `01-intent-alignment` treats these as the required completeness fields:

- date
- time
- party size

The current code does **not** require origin address before Phase 1 can move to
`Waiting for confirmation`.

The written product intent above still says Phase 1 search-ready requirements should include:

- date and time
- origin address
- party size

Until this is resolved, the current code behavior wins for implementation continuity. Future
sessions should not silently reintroduce origin as a required Phase 1 field or rewrite the
specification around the current implementation. They should stop and confirm the intended rule
with the user first.

### Mini Agentic Loop

1. The user sends a free-form request.
2. A single model call maps the request into `UserRequirements`.
3. Deterministic checks decide whether required fields are missing.
4. Deterministic reply construction summarizes the current understanding and asks either for
   confirmation or for the next missing field.
5. If the user confirms, the requirements are marked confirmed.
6. If the user corrects or adds information, the requirements are updated and the loop repeats.
7. If the user response is too vague, the agent asks a targeted clarification question.

### Confirmation Behavior

If the user responds with an affirmative such as:

- yes
- correct
- looks good
- exactly

then the requirements are considered confirmed.

If the user responds with corrections or additions such as:

- no, budget is 80 per person
- not dinner, it's lunch
- it must be quiet
- do not book yet, only recommend

then the constraints are merged and the loop repeats.

If the user reply is not actionable, the agent should ask a targeted follow-up question
instead of proceeding.

### Clarification Priority

When Phase 1 is missing search-ready fields, the agent should ask one targeted question at a
time in this order:

1. date and time
2. origin address
3. party size

The agent should avoid broad follow-ups such as "please clarify the details" when a more
specific question is possible.

### Deterministic Checks Inside Phase 1

Although Phase 2 is the main constraint-checking module, Phase 1 should already perform a
small deterministic completeness check for the required search-ready fields:

- has date and time
- has party size
- can move to confirmation

`Origin address` is intentionally omitted from the current implementation note above because the
requirement is still under discussion. See `Open Product Decision: Origin As A Required Phase 1
Field`.

These are requirement-completeness checks, not restaurant-candidate validators.

### Near-Term Implementation Plan

The current implementation direction should follow these conventions:

1. `agent-core` owns shared session runtime concerns: chat, events, rendered state, and a
   session-backed `AgentContext`.
2. Each planner phase should keep the root package focused on application/bootstrap classes.
3. Each planner phase should use an `.agent` package for the `AgentHandler` and agent-owned
   workflow state.
4. Stable captured user input should live in `.requirements`.
5. Phase-specific logic around filling or checking those requirements should live in a nested
   package such as `.requirements.alignment`.
6. Phase 1 should keep the captured requirements model separate from workflow status and other
   agent-runtime concerns.

### Example Phase 1 Output

```md
## Meal
- Date: 2026-04-11
- Time: 18:00
- Party Size: 4
- Meal Type: dinner
- Purpose: Client dinner
- Budget Per Person: 120
- Noise Level: quiet

## Additional Requirements
- Professional setting

## Cuisine Preferences
- Italian

## Attendees
- Name: Alex | Origin: Union Station | Departure Time: 17:30 | Travel Mode: transit | Max Travel Time: Missing | Max Distance: Missing | Dietary Constraints: vegetarian

## Missing Information
- Date
- Time
- Party Size

## Status
Waiting for confirmation
```

## Phase 2: Constraint Checking

Planned module: `02-constraint-checking`

### Goal

Take the confirmed constraints from Phase 1 and define how each one can be checked
against candidate restaurants.

### Responsibilities

- map each confirmed constraint to a validator type
- define what evidence is needed for that constraint
- define pass, fail, and unsure outcomes
- make explicit which checks are deterministic and which depend on model judgment

### Checking Types

#### Deterministic validators

Use normal code, exact logic, or direct calculations.

Examples:

- budget within policy
- travel time less than a threshold
- restaurant open at requested time
- party size supported
- reservation availability

#### Hybrid validators

Use deterministic evidence gathering plus model interpretation.

Examples:

- vegetarian suitability from menu text
- gluten-free suitability from menu text
- whether the menu offers enough variety for the group
- whether the menu feels appropriate for a business dinner

Pattern:

1. gather the evidence deterministically
2. pass that evidence into the model
3. ask the model to evaluate the constraint and explain its judgment

#### LLM-as-judge validators

Use the model to assess qualitative constraints from text evidence.

Examples:

- business appropriateness from reviews
- noise suitability from reviews and descriptions
- whether other diners report good client-hosting experiences
- whether service quality seems strong enough for a business setting

### Validation Result Shape

Each validator should conceptually return:

- constraint
- validator type
- status: pass, fail, or unsure
- evidence used
- rationale

Example:

```md
## Constraint Check: Vegetarian Suitability
- Type: Hybrid
- Status: Pass
- Evidence: 6 vegetarian items identified on the menu
- Rationale: The menu appears to offer enough vegetarian choices for a business meal.
```

### Teaching Goal

Students should learn to ask, for every constraint:

- what evidence do we need?
- can it be checked deterministically?
- does it require interpretation?
- does it require qualitative judgment?

## Phase 3: Restaurant Planning

Planned module: `03-restaurant-planning`

### Goal

Use the confirmed constraints from Phase 1 and the checking strategy from Phase 2 to
search for restaurant candidates, check them, and produce a ranked shortlist.

### Responsibilities

- search for candidate restaurants
- check candidates against hard and soft constraints
- run in a bounded loop with a max number of turns
- stop early if good candidates are found
- produce either a shortlist or a transparent failure summary

### Inputs To The Loop

The model should receive:

- the full internal constraint set
- explicit constraints
- inferred constraints
- constraint priorities
- checking guidance
- the available tools

The user should only see a simpler summary of the requirements. The loop may operate on
more detailed internal constraint state than what is shown in the confirmation response.

### Turn Limit

- default max turns: 5
- the loop must terminate cleanly if it reaches the limit
- the loop should avoid repeating identical searches unless new information was added

### Constraint Categories In Phase 3

The loop should treat constraints as:

- hard constraints
  - must pass before recommendation
- soft constraints
  - preferred but tradeable
- inferred defaults
  - used unless the user overrides them

The loop should only recommend candidates that satisfy all hard constraints.

### Tool Examples

At minimum, Phase 3 should support tools such as:

- `searchRestaurants`
- `getRestaurantDetails`
- `checkBudgetFit`
- `checkTravelTime`
- `checkDietaryFit`
- `checkBusinessSuitability`

### Candidate Checking Output

Each restaurant should be evaluated in a way that can later be shown to the user.

For each candidate, the system should track:

- hard constraints passed
- hard constraints failed
- soft constraints passed
- soft constraints failed
- strengths
- tradeoffs
- evidence gathered
- overall fit score

### Ranking

Phase 3 should not rank restaurants using a single simplistic rule such as highest rating
or lowest price.

Instead, ranking should be based on a constraint satisfaction score that considers:

- all hard constraints passed or not
- number of soft constraints satisfied
- notable strengths
- notable tradeoffs

The output should be a ranked shortlist, not a final decision.

### Success Output

If acceptable candidates are found, the loop should produce a ranked shortlist similar to:

```md
## Candidate Restaurants

### 1. Restaurant A
- Overall fit: 92
- Hard constraints: 6/6 passed
- Soft constraints: 3/4 passed
- Strengths: quiet, strong vegetarian menu, short commute
- Tradeoffs: slightly more expensive than other options

### 2. Restaurant B
- Overall fit: 88
- Hard constraints: 6/6 passed
- Soft constraints: 2/4 passed
- Strengths: most budget-friendly, easy to reach
- Tradeoffs: atmosphere is less polished
```

### Failure Output

If no acceptable candidate is found within the turn limit, the loop should return a
transparent failure summary.

The summary should include:

- that no full match was found
- which restaurants were evaluated
- how many constraints each candidate satisfied
- which constraints most often blocked success
- what the user could relax or clarify

Example:

```md
## Planning Result

I could not find a restaurant that satisfies all confirmed constraints within 5 planning
steps.

## Candidate Summary
- Restaurant A: satisfied 4/6 constraints
  - Failed: budget, travel time
- Restaurant B: satisfied 5/6 constraints
  - Failed: vegetarian suitability

## What Blocked A Match
- Budget was too restrictive for the requested area and time
- Vegetarian-friendly options were limited in the candidate set

## Next Best Options
- Increase budget
- Expand travel time
- Relax noise preference
- Consider a different neighborhood
```

### Exit Conditions

- shortlist produced -> move to Phase 4
- no valid shortlist -> return failure summary and wait for user changes
- user changes constraints -> go back to Phase 1

## Phase 4: Decision Support

Planned module: `04-decision-support`

### Goal

Let the user explore the shortlisted restaurants, ask follow-up questions, compare
options, and choose what to do next.

### Responsibilities

- answer restaurant-specific questions
- compare candidates
- explain tradeoffs
- let the user pick a restaurant
- allow the user to revise constraints and restart planning

### Example User Questions

- Which one is quietest?
- Compare A and B.
- Show the vegetarian options for restaurant C.
- Which is best for an important client?
- Which one is cheapest?
- Tell me more about the menu.

### Exit Conditions

- user selects a restaurant -> move to Phase 5
- user wants more options -> go back to Phase 3
- user changes constraints -> go back to Phase 1

## Phase 5: Booking

Planned module: `05-booking`

### Goal

Turn the selected option into committed actions such as booking the restaurant and
capturing any final coordination details needed to complete the plan.

### Responsibilities

- confirm the chosen restaurant
- gather any missing booking details
- perform the booking if supported
- support closely related follow-through actions if the sample phase includes them
- return final confirmation

### Failure Handling

If booking fails, the agent should:

- report the failure clearly
- offer alternatives from the shortlist if available
- return to Phase 4 or Phase 3 as appropriate

## Overall State Flow

1. Phase 1: intent alignment
2. Phase 2: constraint checking
3. Phase 3: restaurant planning
4. Phase 4: decision support
5. Phase 5: booking

Fallback transitions:

- changed constraints -> Phase 1
- no valid candidates -> remain around Phase 3 with a failure summary
- user wants deeper restaurant exploration -> Phase 4
- booking failure -> Phase 4 or Phase 3

## Spring AI Mapping

### Phase 1

- use `ChatClient` to map the user request into a Markdown constraint plan
- use deterministic Java checks to determine whether the request is search-ready
- use `ChatClient` again to produce a user-facing confirmation summary

### Phase 2

- map confirmed constraints to checking types in Java
- prepare the checking strategy that later phases will use

### Phase 3

- run an explicit bounded planning loop
- give the model tools for search and constraint checks
- let the model decide which tool to call next within the turn limit

### Phase 4

- use the shortlist and gathered evidence as the conversational context
- let the user ask focused follow-up questions

### Phase 5

- execute booking actions only after the user selects a candidate

## Success Criteria

### Phase 1 succeeds when

- the user intent is captured
- explicit and inferred constraints are separated
- missing information is surfaced
- the user sees an initial orientation message when the session starts
- greeting-only messages are handled with orientation rather than generic clarification
- the minimum required search-ready constraints are checked deterministically
- the user confirms the plan
- the request is not allowed to leave Phase 1 until the required search-ready constraints are present

### Phase 2 succeeds when

- each confirmed constraint has a validator strategy
- deterministic checks are separated from model-based checks
- needed evidence is clear

### Phase 3 succeeds when

- the agent uses tools to evaluate candidates against constraints
- the loop is bounded by a max turn limit
- the agent returns either a shortlist or a transparent failure summary

### Phase 4 succeeds when

- the user can inspect and compare candidates
- the user can either choose one or revise requirements

### Phase 5 succeeds when

- the user-selected restaurant is booked or a clear failure is returned
