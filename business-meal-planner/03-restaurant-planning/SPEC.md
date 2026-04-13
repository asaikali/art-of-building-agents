# 03 Restaurant Planning Spec

## Purpose

Phase 3 builds the agentic planning loop for restaurant selection.

This phase starts after:

- Phase 1 has produced confirmed `UserRequirements`
- Phase 2 already exists and can evaluate a `RestaurantCandidate` against the confirmed
  constraints

Phase 3 is responsible for:

- discovering restaurant candidates
- narrowing the candidate set with cheap deterministic filters first
- inspecting candidate details when needed
- invoking the Phase 2 checks
- deciding which candidates are promising enough to continue exploring
- producing a shortlist or a clear failure summary

## Starting Point

For this phase, assume the code and design work from `02-constraint-checking` already exist.

That means Phase 3 can rely on:

- fake restaurant data from `components/restaurant-data`
- individual check implementations from Phase 2
- `RestaurantCandidateCheckService`
- strongly typed check results

Phase 3 should not reimplement any Phase 2 check logic.

## Main Goal

The key teaching goal of this phase is the agentic loop itself.

Students should see a planning loop that:

1. starts from confirmed requirements
2. uses tools to find a manageable candidate set
3. evaluates candidates
4. decides what to do next
5. stops after a bounded number of planning steps

This phase is not about booking and is not mainly about defining constraints. It is about using
tools and check results to search and narrow the space.

## Non-Goals

Phase 3 should not:

- redefine the Phase 1 requirements model
- redefine the Phase 2 check implementations
- book restaurants
- make irreversible actions
- rely on real external search APIs or live restaurant platforms

## Planning Loop Responsibilities

The Phase 3 planning loop should:

- ask for candidates from fake restaurant data
- use cheap deterministic gating before expensive candidate evaluation
- inspect restaurant details when helpful
- evaluate candidates with the Phase 2 checks
- keep track of what has already been checked
- stop after a fixed planning budget
- return either:
  - a shortlist of promising candidates
  - or a failure summary with the main reasons no candidate was good enough

## Loop Boundaries

The planning loop should be bounded.

The first cut should include limits such as:

- maximum number of candidate evaluations
- maximum number of loop iterations

This is important for the workshop because students should see explicit planning boundaries
rather than an open-ended agent.

## Tool Strategy

Phase 3 should use a small number of tools.

The tool surface should stay easy to understand.

The likely first-cut tools are:

- an availability-based candidate discovery tool
- a restaurant details tool
- a candidate evaluation tool

The evaluation tool should call Phase 2 code rather than exposing every individual check as a
separate planning tool.

## Tool Design Process

This spec intentionally does not fully define each tool yet.

Instead, this phase will follow the same process used in Phase 2:

1. identify one tool
2. define exactly what the tool should do
3. define its input and output shape
4. define how it uses fake data or Phase 2 services
5. only then implement it

Each tool should get its own focused design discussion before code is written.

## Likely First Tool Set

The current expected tool set is:

- `findAvailableRestaurants`
  - find restaurants available for a specific date, time, and party size
  - optionally narrow by neighborhood
- `getRestaurantDetails`
  - load the details for one restaurant candidate
- `checkRestaurantCandidate`
  - invoke `RestaurantCandidateCheckService` and return the typed aggregate check result

These names are placeholders for now. The exact method shapes will be defined one tool at a time.

## Candidate Evaluation Boundary

Phase 3 should treat candidate evaluation as a service call into Phase 2.

That means:

- Phase 3 decides which candidate to evaluate
- Phase 2 decides what each check result is
- a higher planning layer in Phase 3 decides how to use those results

Phase 3 may later decide:

- which results are blocking
- which candidates deserve to stay in the shortlist
- which failures are worth surfacing to the user

But the raw check execution belongs to Phase 2.

## Availability-First Planning

The first cut of the planning loop should avoid checking every restaurant in the dataset.

Instead, it should use a cheap deterministic first pass:

- find restaurants that are available for the requested date, time, and party size
- optionally narrow by neighborhood
- only then run Phase 2 checks on the smaller candidate set

This keeps the planning loop easier to explain and avoids spending LLM-backed checks on
restaurants that cannot satisfy the basic scheduling request.

## Output Of Phase 3

The first cut of Phase 3 should produce one of two outcomes:

- a shortlist of restaurant candidates with their check results
- a failure summary explaining why no strong candidates were found

The failure summary should help teach planning tradeoffs, for example:

- budget ruled out many options
- travel time ruled out distant neighborhoods
- dietary constraints narrowed the set significantly

## Next Steps

The next design step for this phase is to define the first planning tool in detail.

Recommended order:

1. `findAvailableRestaurants`
2. `getRestaurantDetails`
3. `checkRestaurantCandidate`
4. the bounded agent loop that uses them
