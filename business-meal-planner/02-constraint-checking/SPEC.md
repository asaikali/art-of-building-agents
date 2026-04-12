# 02 Constraint Checking Spec

Phase 2 starts after Phase 1 has produced confirmed `UserRequirements`.

This phase introduces a small set of constraint checks grouped by category.

## Deterministic Checks

- `NoiseLevelCheck`
- `BudgetPerPersonCheck`
- `TravelTimeCheck`

The detailed design for `NoiseLevelCheck`, including:

- canonical noise levels
- comparison rule
- validation behavior
- service API
- suggested integration tests

is documented in [noiselevelcheck.md](noiselevelcheck.md).

## Hybrid Checks

- `DietarySuitabilityCheck`

The detailed design for `DietarySuitabilityCheck`, including:

- deterministic evidence gathering
- prompt shape
- short-circuit rules
- service API
- suggested integration tests

is documented in [dietarysuitability.md](dietarysuitability.md).

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
