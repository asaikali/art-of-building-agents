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

- capture shared meal information and attendee-specific requirements
- identify missing information needed to continue
- summarize understanding back to the user and ask for confirmation
- repeat until requirements are confirmed

### Non-goals

- searching for restaurants
- validating restaurants
- ranking candidates
- booking

### Required Search-Ready Constraints

The minimum required fields before Phase 1 can move to confirming are:

- date
- time
- party size

These are checked deterministically. All other fields are optional.

### Open Product Decision: Origin As A Required Phase 1 Field

Current code does **not** require origin address before moving to `Confirming requirements`.
The original product intent included origin as a required field. Until this is resolved, the
current code behavior wins. Future sessions should confirm the intended rule with the user
before reintroducing origin as a required field.

### Greeting Behavior

The agent publishes an initial assistant message when a session is created. If the user sends
only a greeting (hi, hello, can you help), the agent responds with a short orientation message
instead of a generic clarification request.

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

- use `ChatClient` with `.entity(UserRequirements.class)` to extract structured requirements
- use `ChatClient` to suggest an optional follow-up question
- use deterministic Java checks to determine whether required fields are present
- use `ChatClient` to compose a natural reply based on the alignment status

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

## Testing Strategy

### Principles

- No fake or mock objects for LLM-backed components. Fakes for extractors and reply
  writers create maintenance burden and test glue code rather than real behavior.
- Deterministic business logic gets unit tests that run on every build.
- Model-dependent behavior gets integration scenarios that run explicitly and cost tokens.
- Integration scenarios are conditional on environment variables so they never run
  on a plain `mvn test`.

### Tier 1: Deterministic Unit Tests

Run on every build with `mvn test`. No API key required. No model calls.

Test targets:
- Assessor logic (hard gates for required fields)
- Any pure Java business logic added in future phases

These tests should use real objects, not fakes. If a class requires an LLM to construct,
it does not belong in tier 1.

### Tier 2: Model Integration Scenarios

Scenarios are tagged with `@Tag("integration")` and excluded from Maven builds via Surefire's
`<excludedGroups>integration</excludedGroups>` configuration.

- **IDE (IntelliJ, Eclipse):** tags are not filtered by default, so scenarios run normally.
- **Maven (`mvn test`):** Surefire excludes the `integration` tag, scenarios are skipped.
- **Maven explicit:** `mvn test -Dgroups=integration` includes only integration scenarios.

The guard is model-provider agnostic — it does not check for a specific API key. The student
or coding agent configures their provider (OpenAI, Anthropic, Ollama, etc.) via environment
variables or `application.yml` as normal.

Examples:

```bash
# Run all integration scenarios
mvn test -DexcludedGroups= -Dgroups=integration

# Run a specific scenario
mvn test -DexcludedGroups= -Dgroups=integration -Dtest=AlignmentVerificationScenario
```

All model integration scenarios live in a `scenarios` sub-package beneath the feature they
test. For example, Phase 1 alignment scenarios live in:

```
com.example.jarvis.requirements.alignment.scenarios
```

This makes them easy to find in the IDE and to run as a group:

```bash
mvn test -DexcludedGroups= -Dgroups=integration -Dtest="com.example.jarvis.requirements.alignment.scenarios.*"
```

Two kinds of scenario, distinguished by naming convention:

#### Verification scenarios (`*VerificationScenario`)

Purpose: let coding agents confirm the pipeline still works after code changes.

Conventions:
- Class name ends with `VerificationScenario`
- Assert on status and state content per turn using fuzzy matching
- Do not assert on exact reply wording — model output varies
- Keep assertions to status and captured requirements data
- One class per verification scenario

Run only verification scenarios:

```bash
mvn test -DexcludedGroups= -Dgroups=integration -Dtest="*VerificationScenario"
```

#### Walkthrough scenarios (`*Walkthrough`)

Purpose: let students step through the pipeline in a debugger or read the console output.

Conventions:
- Class name ends with `Walkthrough`
- Each turn stored as a separate named variable for debugger inspection
- Print the assistant reply and status to stdout after each turn
- No assertions — these are for observation, not pass/fail
- One class per walkthrough scenario
