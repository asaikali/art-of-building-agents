# Travel Time Estimator

This document describes the fake travel-time design used by `TravelTimeCheck` in Phase 2.

The goal is to make travel-time checking:

- deterministic
- easy to explain
- easy to test
- easy to write scenarios against
- completely independent of real maps or routing APIs

## Scope

`TravelTimeCheck` answers a simple question:

> Given an attendee origin neighborhood, a restaurant neighborhood, a travel mode, and a maximum
> allowed travel time, does this restaurant satisfy the user's travel-time constraint?

This is a deterministic check.

## Design Summary

The design has two parts:

1. a fake `TravelTimeEstimator`
2. a deterministic `TravelTimeCheck`

The estimator returns an estimated number of minutes using:

- an attendee origin neighborhood
- a restaurant neighborhood
- a travel mode

The check then compares the estimate to the attendee's `maxTravelTimeMinutes`.

`TravelTimeEstimator` should not load JSON directly. It should use
`com.example.restaurant.TravelTimeMatrix` from `components/restaurant-data` to look up the
baseline neighborhood-to-neighborhood minutes, then apply the travel-mode adjustment rules
defined in this document.

## Simplifying Assumption

For the workshop, Phase 2 assumes that scenarios provide the attendee origin as a canonical
neighborhood value.

Examples:

- `Financial District`
- `Downtown`
- `King West`
- `Danforth`

We do not try to geocode free-form origin strings in this phase.

## Why This Is Fake

We do not want:

- geocoding
- real route planning
- API keys
- variable network responses
- flaky tests

Instead, we use a workshop-friendly neighborhood matrix and simple mode adjustments.

## Supported Neighborhoods

The estimator should use the neighborhoods already present in `restaurant-data`:

- `Financial District`
- `Downtown`
- `St. Lawrence`
- `Harbourfront`
- `Entertainment District`
- `King West`
- `Chinatown`
- `Koreatown`
- `Yorkville`
- `Midtown`
- `Danforth`

Both the attendee origin and the restaurant location should be represented using values from
this set.

## Naming Note

This document uses `Danforth` instead of `Danforth / Greektown`.

The fake restaurant data now uses `Danforth` consistently in the restaurant
JSON files so the neighborhood names stay simple and canonical across the workshop.

## Matrix Storage

The neighborhood travel-time matrix is stored as JSON in:

`components/restaurant-data/src/main/resources/restaurant-data/travel-time-matrix.json`

This JSON is loaded by `com.example.restaurant.TravelTimeMatrix` in the `restaurant-data`
component. Phase 2's `TravelTimeEstimator` should depend on that component rather than parsing
the JSON itself.

## Baseline Time Matrix

The matrix below defines the baseline travel time in minutes before travel-mode adjustments.

If the attendee origin neighborhood and restaurant neighborhood are the same, the baseline
travel time is always `10` minutes.

| From / To | Financial District | Downtown | St. Lawrence | Harbourfront | Entertainment District | King West | Chinatown | Koreatown | Yorkville | Midtown | Danforth |
|----------|--------------------:|---------:|-------------:|-------------:|-----------------------:|----------:|----------:|-----------:|----------:|--------:|---------------------:|
| `Financial District` | 10 | 12 | 14 | 15 | 13 | 14 | 16 | 20 | 18 | 24 | 30 |
| `Downtown` | 12 | 10 | 13 | 14 | 12 | 13 | 15 | 18 | 17 | 22 | 29 |
| `St. Lawrence` | 14 | 13 | 10 | 16 | 17 | 18 | 18 | 22 | 21 | 27 | 24 |
| `Harbourfront` | 15 | 14 | 16 | 10 | 12 | 15 | 18 | 24 | 22 | 28 | 31 |
| `Entertainment District` | 13 | 12 | 17 | 12 | 10 | 11 | 14 | 18 | 18 | 24 | 31 |
| `King West` | 14 | 13 | 18 | 15 | 11 | 10 | 13 | 17 | 19 | 24 | 32 |
| `Chinatown` | 16 | 15 | 18 | 18 | 14 | 13 | 10 | 12 | 18 | 20 | 28 |
| `Koreatown` | 20 | 18 | 22 | 24 | 18 | 17 | 12 | 10 | 17 | 18 | 25 |
| `Yorkville` | 18 | 17 | 21 | 22 | 18 | 19 | 18 | 17 | 10 | 14 | 25 |
| `Midtown` | 24 | 22 | 27 | 28 | 24 | 24 | 20 | 18 | 14 | 10 | 22 |
| `Danforth` | 30 | 29 | 24 | 31 | 31 | 32 | 28 | 25 | 25 | 22 | 10 |

These numbers do not need to be realistic in every edge case. They need to be:

- stable
- plausible
- easy to reason about

## Travel Mode Adjustments

After reading the baseline minutes from the matrix, apply the following rules:

- `WALKING`
  - final minutes = `baseMinutes`
- `TRANSIT`
  - final minutes = `round(baseMinutes * 1.3)`
- `DRIVING`
  - final minutes = `max(8, round(baseMinutes * 0.9))`
- `TAXI`
  - final minutes = `max(8, round(baseMinutes * 1.0))`

These rules are intentionally simple.

They are not trying to simulate Toronto accurately. They are trying to create:

- predictable results
- visible differences between travel modes
- clear teaching examples

## Estimator Formula

Conceptually:

```text
baseMinutes = matrix[originNeighborhood][restaurantNeighborhood]
finalMinutes = applyTravelModeAdjustment(baseMinutes, travelMode)
```

## TravelTimeEstimatorService

Phase 2 should implement a `TravelTimeEstimatorService` in the constraint-checking module.

Suggested package:

`com.example.jarvis.constraintchecking.travel`

Suggested responsibility:

- depend on `com.example.restaurant.TravelTimeMatrix`
- return one final estimated travel time in minutes
- hide matrix lookup and travel-mode adjustment logic from callers

Suggested method:

```java
int estimateMinutes(
    String originNeighborhood,
    String restaurantNeighborhood,
    TravelMode travelMode)
```

### Behavior

The service should:

1. validate all required inputs
2. throw `IllegalArgumentException` if:
   - `originNeighborhood` is null or blank
   - `restaurantNeighborhood` is null or blank
   - `travelMode` is null
3. ask `TravelTimeMatrix` for the base neighborhood-to-neighborhood minutes
4. if a base time is found, apply the travel-mode adjustment
5. if no base time is found, return a fixed fallback estimate of `60`

### Fallback Rule

If the matrix does not contain a value for a neighborhood pair, the estimator should return:

```text
60
```

This fallback is the final estimated travel time.

The estimator should not apply the travel-mode adjustment to the fallback.

### Suggested Implementation Shape

```java
@Service
public class TravelTimeEstimatorService {

  private static final int FALLBACK_ESTIMATED_MINUTES = 60;

  private final TravelTimeMatrix travelTimeMatrix;

  public TravelTimeEstimatorService(TravelTimeMatrix travelTimeMatrix) {
    this.travelTimeMatrix = travelTimeMatrix;
  }

  public int estimateMinutes(
      String originNeighborhood, String restaurantNeighborhood, TravelMode travelMode) {
    validateInputs(originNeighborhood, restaurantNeighborhood, travelMode);

    return travelTimeMatrix
        .findBaseMinutes(originNeighborhood, restaurantNeighborhood)
        .map(baseMinutes -> applyTravelMode(baseMinutes, travelMode))
        .orElse(FALLBACK_ESTIMATED_MINUTES);
  }

  private void validateInputs(
      String originNeighborhood, String restaurantNeighborhood, TravelMode travelMode) {
    if (originNeighborhood == null || originNeighborhood.isBlank()) {
      throw new IllegalArgumentException("originNeighborhood must be provided");
    }
    if (restaurantNeighborhood == null || restaurantNeighborhood.isBlank()) {
      throw new IllegalArgumentException("restaurantNeighborhood must be provided");
    }
    if (travelMode == null) {
      throw new IllegalArgumentException("travelMode must be provided");
    }
  }

  private int applyTravelMode(int baseMinutes, TravelMode travelMode) {
    return switch (travelMode) {
      case WALKING -> baseMinutes;
      case TRANSIT -> (int) Math.round(baseMinutes * 1.3);
      case DRIVING -> Math.max(8, (int) Math.round(baseMinutes * 0.9));
      case TAXI -> Math.max(8, (int) Math.round(baseMinutes * 1.0));
    };
  }
}
```

## Suggested Unit Test

`TravelTimeEstimatorService` should be tested using the real `TravelTimeMatrix` loaded from the
fake JSON data, not a mock.

Suggested test shape:

```java
package com.example.jarvis.constraintchecking.travel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jarvis.requirements.TravelMode;
import com.example.restaurant.TravelTimeMatrix;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TravelTimeEstimatorServiceTest {

  private TravelTimeEstimatorService service;

  @BeforeEach
  void setUp() {
    var travelTimeMatrix = new TravelTimeMatrix(new ObjectMapper());
    service = new TravelTimeEstimatorService(travelTimeMatrix);
  }

  @Test
  @DisplayName("Returns same-neighborhood walking estimate from the matrix")
  void returnsWalkingEstimateForSameNeighborhood() {
    int result =
        service.estimateMinutes("Financial District", "Financial District", TravelMode.WALKING);

    assertThat(result).isEqualTo(10);
  }

  @Test
  @DisplayName("Applies transit adjustment to the base travel time")
  void appliesTransitAdjustment() {
    int result = service.estimateMinutes("Yorkville", "Koreatown", TravelMode.TRANSIT);

    assertThat(result).isEqualTo(22);
  }

  @Test
  @DisplayName("Applies driving adjustment to the base travel time")
  void appliesDrivingAdjustment() {
    int result =
        service.estimateMinutes("Harbourfront", "Entertainment District", TravelMode.DRIVING);

    assertThat(result).isEqualTo(11);
  }

  @Test
  @DisplayName("Returns fallback estimate when the neighborhood pair is missing")
  void returnsFallbackWhenNeighborhoodPairIsMissing() {
    int result = service.estimateMinutes("Unknown", "Downtown", TravelMode.TRANSIT);

    assertThat(result).isEqualTo(60);
  }

  @Test
  @DisplayName("Throws when origin neighborhood is null")
  void throwsWhenOriginNeighborhoodIsNull() {
    assertThatThrownBy(() -> service.estimateMinutes(null, "Downtown", TravelMode.WALKING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("originNeighborhood must be provided");
  }

  @Test
  @DisplayName("Throws when origin neighborhood is blank")
  void throwsWhenOriginNeighborhoodIsBlank() {
    assertThatThrownBy(() -> service.estimateMinutes("  ", "Downtown", TravelMode.WALKING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("originNeighborhood must be provided");
  }

  @Test
  @DisplayName("Throws when restaurant neighborhood is null")
  void throwsWhenRestaurantNeighborhoodIsNull() {
    assertThatThrownBy(
            () -> service.estimateMinutes("Financial District", null, TravelMode.WALKING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantNeighborhood must be provided");
  }

  @Test
  @DisplayName("Throws when restaurant neighborhood is blank")
  void throwsWhenRestaurantNeighborhoodIsBlank() {
    assertThatThrownBy(
            () -> service.estimateMinutes("Financial District", " ", TravelMode.WALKING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantNeighborhood must be provided");
  }

  @Test
  @DisplayName("Throws when travel mode is null")
  void throwsWhenTravelModeIsNull() {
    assertThatThrownBy(() -> service.estimateMinutes("Financial District", "Downtown", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("travelMode must be provided");
  }
}
```

## TravelTimeCheck Behavior

`TravelTimeCheck` should:

1. read the attendee origin neighborhood
2. read the attendee travel mode
3. read the attendee `maxTravelTimeMinutes`
4. read the restaurant neighborhood
5. ask `TravelTimeEstimator` for estimated minutes
6. compare estimated minutes to the attendee limit

Suggested result rules:

- `PASS`
  - estimated minutes are less than or equal to `maxTravelTimeMinutes`
- `FAIL`
  - estimated minutes are greater than `maxTravelTimeMinutes`
- `UNSURE`
  - required input is missing or unsupported

Required inputs for the check:

- attendee origin neighborhood
- attendee travel mode
- attendee `maxTravelTimeMinutes`
- restaurant neighborhood

## Scenario Examples

Example 1:

- origin neighborhood: `Financial District`
- restaurant neighborhood: `Financial District`
- travel mode: `WALKING`
- max travel time: `10`

Calculation:

- base = `10`
- walking = `10`
- result = `PASS`

Example 2:

- origin neighborhood: `Danforth`
- restaurant neighborhood: `King West`
- travel mode: `WALKING`
- max travel time: `25`

Calculation:

- base = `32`
- walking = `32`
- result = `FAIL`

Example 3:

- origin neighborhood: `Harbourfront`
- restaurant neighborhood: `Entertainment District`
- travel mode: `DRIVING`
- max travel time: `12`

Calculation:

- base = `12`
- driving = `max(8, round(12 * 0.9))` = `11`
- result = `PASS`

Example 4:

- origin neighborhood: `Yorkville`
- restaurant neighborhood: `Koreatown`
- travel mode: `TRANSIT`
- max travel time: `20`

Calculation:

- base = `17`
- transit = `round(17 * 1.3)` = `22`
- result = `FAIL`

## Why This Works Well In A Workshop

This design gives students a deterministic but still "computed" check.

It teaches:

- separating data lookup from business logic
- using a helper service (`TravelTimeEstimator`) inside a checker
- returning structured evidence
- writing predictable tests and walkthrough scenarios

It also keeps the implementation small enough to understand in one sitting.
