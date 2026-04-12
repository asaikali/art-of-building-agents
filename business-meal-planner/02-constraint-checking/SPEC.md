# 02 Constraint Checking Spec

Phase 2 starts after Phase 1 has produced confirmed `UserRequirements`.

This phase introduces a small set of constraint checks grouped by category.

## Deterministic Checks

- `NoiseLevelCheck`
- `BudgetPerPersonCheck`
- `TravelTimeCheck`

## Hybrid Checks

- `DietarySuitabilityCheck`

## LLM-As-Judge Checks

- `BusinessMealSuitabilityCheck`

## Travel Time Estimator

`TravelTimeCheck` should use a fake deterministic `TravelTimeEstimator`.

The detailed design for the estimator, including:

- neighborhood-based travel-time matrix
- travel-mode adjustments
- fallback behavior
- service API
- suggested tests

is documented in [travelestimator.md](travelestimator.md).
