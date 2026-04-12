# Business Meal Suitability Check

This document describes the design of `BusinessMealSuitabilityCheck` in Phase 2.

The goal is to demonstrate a focused LLM-as-judge check that uses restaurant metadata and the
meal purpose to decide whether a venue looks like a good fit.

## Scope

`BusinessMealSuitabilityCheck` answers a simple question:

> Given the user's stated meal purpose and the restaurant metadata, does the restaurant appear
> suitable for that meal context?

This is an LLM-as-judge check.

## Why This Is An LLM-As-Judge Check

This check is intentionally qualitative.

It is not trying to compute:

- travel time
- numeric budget fit
- dietary coverage

Instead, it asks the model to make a judgment about overall fit using coarse evidence such as:

- restaurant description
- neighborhood
- noise level
- price range
- stated meal purpose

## Inputs

The check should use:

- `Meal`
- `restaurantId`

The key meal fields are:

- `mealType`
- `purpose`
- `noiseLevel`
- `budgetPerPerson`

The restaurant metadata comes from `RestaurantService`.

## Why Purpose Matters

The check should use `meal.getPurpose()` as a central input.

That lets the model judge fit for cases like:

- client dinner
- internal team lunch
- hosted business meal
- celebratory meal
- quiet working lunch

This is better than asking only whether a restaurant is generally appropriate for business dining.

## Validation Rules

The check should validate `restaurantId`.

If `restaurantId` is null or blank, it should throw:

- `IllegalArgumentException`

If the restaurant id is unknown, it should throw:

- `IllegalArgumentException`

`Meal` itself may be null. In that case, the model should still judge fit using the restaurant
metadata alone, but the result will naturally be weaker.

## Prompt Strategy

The model should receive:

- compact structured meal context
- compact structured restaurant context

The prompt should ask whether the restaurant is suitable for the user's stated meal purpose and
context.

The model should be instructed to:

- use only the provided metadata
- focus on overall fit, atmosphere, conversation suitability, and business appropriateness
- avoid inventing facts
- be conservative when evidence is weak
- return `PASS`, `FAIL`, or `UNSURE`

## Suggested Output Shape

The first cut should keep the output small.

Suggested fields:

- `status`
- `rationale`

Example:

```json
{
  "status": "PASS",
  "rationale": "The restaurant appears polished, professional, and quiet enough for a client dinner."
}
```

## Suggested Service Design

Suggested package:

`com.example.jarvis.constraintchecking.businessmeal`

Suggested class:

- `BusinessMealSuitabilityCheck`

Suggested dependencies:

- `RestaurantService`
- `ChatClient`

## Suggested Implementation Shape

```java
package com.example.jarvis.constraintchecking.businessmeal;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.Meal;
import com.example.restaurant.RestaurantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class BusinessMealSuitabilityCheck {

  private final RestaurantService restaurantService;
  private final ChatClient chatClient;

  public BusinessMealSuitabilityCheck(
      RestaurantService restaurantService, ChatClient.Builder chatClientBuilder) {
    this.restaurantService = restaurantService;
    this.chatClient = chatClientBuilder.build();
  }

  public BusinessMealSuitabilityResult check(Meal meal, String restaurantId) {
    if (restaurantId == null || restaurantId.isBlank()) {
      throw new IllegalArgumentException("restaurantId must be provided");
    }

    var restaurant =
        restaurantService
            .findById(restaurantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown restaurant id: " + restaurantId));

    var mealContext =
        meal == null
            ? null
            : new MealContext(
                meal.getMealType(),
                meal.getPurpose(),
                meal.getNoiseLevel(),
                meal.getBudgetPerPerson());

    var restaurantContext =
        new RestaurantContext(
            restaurant.name(),
            restaurant.neighborhood(),
            restaurant.noiseLevel(),
            restaurant.priceRangePerPerson(),
            restaurant.description());

    return chatClient
        .prompt()
        .system(
            """
            You are judging whether a restaurant appears suitable for the user's stated meal purpose and context.

            Rules:
            - Use only the meal context and restaurant details provided.
            - Focus on whether the venue fits the meal purpose, likely atmosphere,
              conversation suitability, and business appropriateness.
            - Do not invent facts beyond the provided restaurant metadata.
            - Be conservative when the evidence is weak.
            - Return PASS, FAIL, or UNSURE.
            """)
        .user(
            u ->
                u.text(
                        """
                    <mealContext>
                    {mealContext}
                    </mealContext>

                    <restaurantContext>
                    {restaurantContext}
                    </restaurantContext>
                    """)
                    .param("mealContext", JsonUtils.toJson(mealContext))
                    .param("restaurantContext", JsonUtils.toJson(restaurantContext)))
        .call()
        .entity(BusinessMealSuitabilityResult.class);
  }

  private record MealContext(
      Object mealType,
      String purpose,
      Object requestedNoiseLevel,
      Object budgetPerPerson) {}

  private record RestaurantContext(
      String name,
      String neighborhood,
      String noiseLevel,
      Object priceRangePerPerson,
      String description) {}
}
```

## Suggested Supporting Types

```java
package com.example.jarvis.constraintchecking.businessmeal;

public enum BusinessMealSuitabilityStatus {
  PASS,
  FAIL,
  UNSURE
}
```

```java
package com.example.jarvis.constraintchecking.businessmeal;

public record BusinessMealSuitabilityResult(
    BusinessMealSuitabilityStatus status,
    String rationale) {}
```

## Suggested Integration Test Shape

For this workshop, integration tests are enough.

Suggested scenario coverage:

- client dinner at a polished quiet restaurant -> likely `PASS`
- internal team lunch at a casual loud venue -> possibly `PASS` or `UNSURE`
- client dinner at a loud casual party-style venue -> likely `FAIL` or `UNSURE`
- blank `restaurantId` -> throw `IllegalArgumentException`

Example shape:

```java
package com.example.jarvis.constraintchecking.businessmeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.MealType;
import com.example.jarvis.requirements.NoiseLevel;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
class BusinessMealSuitabilityCheckIntegrationTest {

  @Autowired private BusinessMealSuitabilityCheck check;

  @Test
  @DisplayName("Returns PASS or UNSURE for a polished client dinner venue")
  void returnsPassOrUnsureForPolishedClientDinnerVenue() {
    var meal = new Meal();
    meal.setMealType(MealType.DINNER);
    meal.setPurpose("client dinner");
    meal.setNoiseLevel(NoiseLevel.QUIET);
    meal.setBudgetPerPerson(new BigDecimal("120"));

    var result = check.check(meal, "canoe");

    assertThat(result.status()).isIn(
        BusinessMealSuitabilityStatus.PASS,
        BusinessMealSuitabilityStatus.UNSURE);
  }

  @Test
  @DisplayName("Returns FAIL or UNSURE for a loud venue for a client dinner")
  void returnsFailOrUnsureForLoudVenueForClientDinner() {
    var meal = new Meal();
    meal.setMealType(MealType.DINNER);
    meal.setPurpose("client dinner");
    meal.setNoiseLevel(NoiseLevel.QUIET);
    meal.setBudgetPerPerson(new BigDecimal("120"));

    var result = check.check(meal, "baro");

    assertThat(result.status()).isIn(
        BusinessMealSuitabilityStatus.FAIL,
        BusinessMealSuitabilityStatus.UNSURE);
  }

  @Test
  @DisplayName("Throws when restaurant id is blank")
  void throwsWhenRestaurantIdIsBlank() {
    assertThatThrownBy(() -> check.check(null, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantId must be provided");
  }
}
```

## Why This Works Well In A Workshop

This check teaches:

- how to use structured metadata as evidence for an LLM judgment
- how to make the prompt narrow and context-driven
- why purpose matters in venue selection
- how to keep a qualitative check small and inspectable

It also complements the other Phase 2 checks well:

- deterministic checks cover hard numerical and categorical constraints
- the hybrid check covers menu-based dietary suitability
- this check covers overall qualitative fit for the stated meal purpose
