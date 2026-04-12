# Budget Per Person Check

This document describes the design of `BudgetPerPersonCheck` in Phase 2.

The goal is to demonstrate a simple deterministic check that compares the user's per-person
budget to a restaurant's published per-person price range.

## Scope

`BudgetPerPersonCheck` answers a simple question:

> Given a requested per-person budget and a restaurant's published per-person price range,
> is the restaurant clearly within budget, clearly outside budget, or only a possible fit?

This is a deterministic check.

## Inputs

The check uses:

- requested per-person budget from Phase 1
- `restaurantId`
- the restaurant's structured `priceRangePerPerson` from `components/restaurant-data`

## Price Range Shape

Restaurant pricing is stored as a nested JSON object and mapped into `PriceRange`.

Example:

```json
{
  "priceRangePerPerson": {
    "tier": "$$$",
    "min": 70,
    "max": 110,
    "currency": "CAD",
    "label": "$$$ (70–110 CAD)"
  }
}
```

The check should compare only the numeric `min` and `max` values.

## Comparison Rule

The check should return one of:

- `PASS`
- `FAIL`
- `MAYBE`

### Meaning Of Each Status

- `PASS`
  - the requested budget is greater than or equal to the restaurant's upper bound
- `FAIL`
  - the requested budget is below the restaurant's lower bound
- `MAYBE`
  - the requested budget falls inside the restaurant's published range

This is intentionally simple and works well with coarse fake pricing data.

## Validation Rules

The check should validate `restaurantId`.

If `restaurantId` is null or blank, it should throw:

- `IllegalArgumentException`

If the restaurant id is unknown, it should throw:

- `IllegalArgumentException`

If the restaurant price range is missing, it should throw:

- `IllegalStateException`

If the restaurant price range min/max values are missing, it should throw:

- `IllegalStateException`

If the requested budget is null, the check should return:

- `PASS`

because no budget constraint was provided.

## Suggested Service Design

Suggested package:

`com.example.jarvis.constraintchecking.budget`

Suggested class:

- `BudgetPerPersonCheck`

Suggested dependency:

- `RestaurantService`

## Suggested Implementation Shape

```java
package com.example.jarvis.constraintchecking.budget;

import com.example.restaurant.RestaurantService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BudgetPerPersonCheck {

  private final RestaurantService restaurantService;

  public BudgetPerPersonCheck(RestaurantService restaurantService) {
    this.restaurantService = restaurantService;
  }

  public BudgetPerPersonCheckResult check(BigDecimal requestedBudgetPerPerson, String restaurantId) {
    if (restaurantId == null || restaurantId.isBlank()) {
      throw new IllegalArgumentException("restaurantId must be provided");
    }

    if (requestedBudgetPerPerson == null) {
      return new BudgetPerPersonCheckResult(
          BudgetPerPersonCheckStatus.PASS,
          "No per-person budget was provided.");
    }

    var restaurant =
        restaurantService
            .findById(restaurantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown restaurant id: " + restaurantId));

    var priceRange = restaurant.priceRangePerPerson();
    if (priceRange == null) {
      throw new IllegalStateException("Restaurant price range must be present");
    }
    if (priceRange.min() == null || priceRange.max() == null) {
      throw new IllegalStateException(
          "Restaurant price range min/max must be present for: " + restaurantId);
    }

    var min = BigDecimal.valueOf(priceRange.min());
    var max = BigDecimal.valueOf(priceRange.max());

    if (requestedBudgetPerPerson.compareTo(max) >= 0) {
      return new BudgetPerPersonCheckResult(
          BudgetPerPersonCheckStatus.PASS,
          "Budget %s covers the restaurant range %s-%s %s."
              .formatted(
                  requestedBudgetPerPerson.toPlainString(),
                  min.toPlainString(),
                  max.toPlainString(),
                  priceRange.currency()));
    }

    if (requestedBudgetPerPerson.compareTo(min) < 0) {
      return new BudgetPerPersonCheckResult(
          BudgetPerPersonCheckStatus.FAIL,
          "Budget %s is below the restaurant range %s-%s %s."
              .formatted(
                  requestedBudgetPerPerson.toPlainString(),
                  min.toPlainString(),
                  max.toPlainString(),
                  priceRange.currency()));
    }

    return new BudgetPerPersonCheckResult(
        BudgetPerPersonCheckStatus.MAYBE,
        "Budget %s falls inside the restaurant range %s-%s %s."
            .formatted(
                requestedBudgetPerPerson.toPlainString(),
                min.toPlainString(),
                max.toPlainString(),
                priceRange.currency()));
  }
}
```

## Suggested Supporting Types

```java
package com.example.jarvis.constraintchecking.budget;

public enum BudgetPerPersonCheckStatus {
  PASS,
  FAIL,
  MAYBE
}
```

```java
package com.example.jarvis.constraintchecking.budget;

public record BudgetPerPersonCheckResult(
    BudgetPerPersonCheckStatus status,
    String rationale) {}
```

## Suggested Integration Test Shape

For this workshop, integration tests are enough.

Suggested test shape:

```java
package com.example.jarvis.constraintchecking.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
class BudgetPerPersonCheckIntegrationTest {

  @Autowired private BudgetPerPersonCheck check;

  @Test
  @DisplayName("Returns PASS when no budget is provided")
  void returnsPassWhenNoBudgetIsProvided() {
    var result = check.check(null, "canoe");

    assertThat(result.status()).isEqualTo(BudgetPerPersonCheckStatus.PASS);
  }

  @Test
  @DisplayName("Returns PASS when the budget covers the restaurant's upper bound")
  void returnsPassWhenBudgetCoversUpperBound() {
    var result = check.check(new BigDecimal("120"), "canoe");

    assertThat(result.status()).isEqualTo(BudgetPerPersonCheckStatus.PASS);
  }

  @Test
  @DisplayName("Returns FAIL when the budget is below the restaurant's lower bound")
  void returnsFailWhenBudgetIsBelowLowerBound() {
    var result = check.check(new BigDecimal("60"), "canoe");

    assertThat(result.status()).isEqualTo(BudgetPerPersonCheckStatus.FAIL);
  }

  @Test
  @DisplayName("Returns MAYBE when the budget falls inside the restaurant's range")
  void returnsMaybeWhenBudgetFallsInsideRange() {
    var result = check.check(new BigDecimal("80"), "canoe");

    assertThat(result.status()).isEqualTo(BudgetPerPersonCheckStatus.MAYBE);
  }

  @Test
  @DisplayName("Throws when restaurant id is blank")
  void throwsWhenRestaurantIdIsBlank() {
    assertThatThrownBy(() -> check.check(new BigDecimal("80"), " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantId must be provided");
  }
}
```

## Why This Works Well In A Workshop

This check teaches:

- using structured fake data instead of parsing display text
- applying a simple deterministic comparison rule
- returning a meaningful intermediate status with `MAYBE`
- keeping validation and business logic explicit

It also pairs well with the price-range data normalization already added to
`components/restaurant-data`.
