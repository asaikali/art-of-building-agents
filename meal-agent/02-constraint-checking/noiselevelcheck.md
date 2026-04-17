# Noise Level Check

This document describes the design of `NoiseLevelCheck` in Phase 2.

The goal is to demonstrate a simple deterministic check that compares the user's requested
noise level to a restaurant's stored noise level.

## Scope

`NoiseLevelCheck` answers a simple question:

> Given a requested noise level and a restaurant's noise level, does the restaurant satisfy
> the user's preference?

This is a deterministic check.

## Inputs

The check uses:

- requested `NoiseLevel` from Phase 1
- `restaurantId`
- the restaurant's `noiseLevel` from `components/restaurant-data`

## Canonical Noise Levels

Phase 1 should use three canonical values:

- `QUIET`
- `MODERATE`
- `LOUD`

The fake restaurant data should use the matching lowercase values:

- `quiet`
- `moderate`
- `loud`

## Comparison Rule

The requested noise level should be treated as the maximum acceptable noise level.

That means:

- if the user requests `QUIET`, only `QUIET` passes
- if the user requests `MODERATE`, `QUIET` and `MODERATE` pass
- if the user requests `LOUD`, any restaurant passes

This rule is implemented by ranking the noise levels:

- `QUIET` = `1`
- `MODERATE` = `2`
- `LOUD` = `3`

The check passes when:

```text
actualNoiseRank <= requestedNoiseRank
```

## Validation Rules

The check should validate `restaurantId`.

If `restaurantId` is null or blank, it should throw:

- `IllegalArgumentException`

If the restaurant id is unknown, it should throw:

- `IllegalArgumentException`

If the restaurant noise level is missing or cannot be mapped to the enum, it should throw:

- `IllegalStateException`

If the requested noise level is null, the check should return:

- `PASS`

because no noise preference was provided.

## Suggested Service Design

Suggested package:

`com.example.jarvis.constraintchecking.noise`

Suggested class:

- `NoiseLevelCheck`

Suggested dependency:

- `RestaurantService`

## Suggested Implementation Shape

```java
package com.example.jarvis.constraintchecking.noise;

import com.example.jarvis.requirements.NoiseLevel;
import com.example.restaurant.RestaurantService;
import org.springframework.stereotype.Component;

@Component
public class NoiseLevelCheck {

  private final RestaurantService restaurantService;

  public NoiseLevelCheck(RestaurantService restaurantService) {
    this.restaurantService = restaurantService;
  }

  public NoiseLevelCheckResult check(NoiseLevel requestedNoiseLevel, String restaurantId) {
    if (restaurantId == null || restaurantId.isBlank()) {
      throw new IllegalArgumentException("restaurantId must be provided");
    }

    if (requestedNoiseLevel == null) {
      return new NoiseLevelCheckResult(
          NoiseLevelCheckStatus.PASS,
          "No noise level preference was provided.");
    }

    var restaurant =
        restaurantService
            .findById(restaurantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown restaurant id: " + restaurantId));

    var actualNoiseLevel = NoiseLevel.fromString(restaurant.noiseLevel());

    if (actualNoiseLevel == null) {
      throw new IllegalStateException(
          "Restaurant noise level is missing or invalid for: " + restaurantId);
    }

    boolean passes = toRank(actualNoiseLevel) <= toRank(requestedNoiseLevel);

    return new NoiseLevelCheckResult(
        passes ? NoiseLevelCheckStatus.PASS : NoiseLevelCheckStatus.FAIL,
        "Requested %s, restaurant is %s."
            .formatted(requestedNoiseLevel.name(), actualNoiseLevel.name()));
  }

  private int toRank(NoiseLevel noiseLevel) {
    return switch (noiseLevel) {
      case QUIET -> 1;
      case MODERATE -> 2;
      case LOUD -> 3;
    };
  }
}
```

## Suggested Supporting Types

```java
package com.example.jarvis.constraintchecking.noise;

public enum NoiseLevelCheckStatus {
  PASS,
  FAIL
}
```

```java
package com.example.jarvis.constraintchecking.noise;

public record NoiseLevelCheckResult(
    NoiseLevelCheckStatus status,
    String rationale) {}
```

## Suggested Integration Test Shape

For this workshop, integration tests are enough.

Suggested test shape:

```java
package com.example.jarvis.constraintchecking.noise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jarvis.requirements.NoiseLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
class NoiseLevelCheckIntegrationTest {

  @Autowired private NoiseLevelCheck check;

  @Test
  @DisplayName("Returns PASS when no noise preference is provided")
  void returnsPassWhenNoNoisePreferenceIsProvided() {
    var result = check.check(null, "canoe");

    assertThat(result.status()).isEqualTo(NoiseLevelCheckStatus.PASS);
  }

  @Test
  @DisplayName("Returns PASS when quiet is requested and restaurant is quiet")
  void returnsPassForQuietRestaurant() {
    var result = check.check(NoiseLevel.QUIET, "canoe");

    assertThat(result.status()).isEqualTo(NoiseLevelCheckStatus.PASS);
  }

  @Test
  @DisplayName("Returns FAIL when quiet is requested and restaurant is loud")
  void returnsFailForLoudRestaurantWhenQuietIsRequested() {
    var result = check.check(NoiseLevel.QUIET, "baro");

    assertThat(result.status()).isEqualTo(NoiseLevelCheckStatus.FAIL);
  }

  @Test
  @DisplayName("Returns PASS when moderate is requested and restaurant is quiet")
  void returnsPassForQuietRestaurantWhenModerateIsRequested() {
    var result = check.check(NoiseLevel.MODERATE, "canoe");

    assertThat(result.status()).isEqualTo(NoiseLevelCheckStatus.PASS);
  }

  @Test
  @DisplayName("Throws when restaurant id is blank")
  void throwsWhenRestaurantIdIsBlank() {
    assertThatThrownBy(() -> check.check(NoiseLevel.QUIET, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantId must be provided");
  }
}
```

## Why This Works Well In A Workshop

This check teaches:

- using a stable enum from Phase 1 as input
- reading fake restaurant data
- applying a simple deterministic ranking rule
- returning a structured pass/fail result

It is also small enough to understand quickly before moving on to the hybrid and LLM-based
checks.
