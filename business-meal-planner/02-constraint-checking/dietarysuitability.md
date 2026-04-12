# Dietary Suitability Check

This document describes the design of `DietarySuitabilityCheck` in Phase 2.

The goal is to demonstrate a true hybrid check:

- Java gathers the evidence deterministically
- the model judges whether that evidence is good enough

## Scope

`DietarySuitabilityCheck` answers a simple question:

> Given the party's dietary constraints and a restaurant's menu, does the restaurant appear
> suitable for the group?

This is a hybrid check.

## Why This Is Hybrid

The check has two distinct parts:

1. deterministic evidence gathering
2. model-based suitability judgment

The Java side should:

- read the dietary constraints from the party
- load the restaurant menu
- serialize the menu into a compact JSON prompt payload
- pass that evidence to the model

The model should:

- judge whether the restaurant appears suitable
- return a structured result
- explain the decision briefly

## Inputs

The check should ultimately work from:

- confirmed `UserRequirements`
- the restaurant being checked
- the restaurant menu

For the core dietary evaluation logic, the important inputs are:

- `List<DietaryConstraint>`
- `RestaurantMenu`

## Deterministic Evidence Gathering

Before calling the model, Java should gather the relevant evidence.

### Dietary Constraints

The check should:

- collect dietary constraints across all attendees
- remove duplicates
- ignore `NONE`

If multiple attendees have the same dietary constraint, it only needs to appear once in the
evidence sent to the model.

### Menu Evidence

The first cut does not need a separate evidence model.

The check can use the existing `RestaurantMenu` shape from `components/restaurant-data`
and serialize it to pretty JSON using `JsonUtils.toJson(...)`.

That keeps the implementation smaller and avoids creating extra menu-evidence classes that
duplicate the existing menu model.

### Evidence Goal

The Java side should not try to decide dietary suitability.

Its job is only to provide the model with:

- the dietary needs
- the relevant menu evidence

### Restaurant Id Validation

The check should validate `restaurantId` before looking up menu data.

If `restaurantId` is null or blank, it should throw:

- `IllegalArgumentException`

## Short-Circuit Rules

The check should not always call the model.

### No Dietary Constraints

If the party has no dietary constraints after removing duplicates and `NONE`, the check should
return:

- `PASS`

No model call is needed.

### Missing Menu

If the restaurant has no menu data, the check should return:

- `UNSURE`

No model call is needed.

This is not a hard failure. It means the system does not have enough evidence to judge.

## LLM Judgment

If there are dietary constraints and menu data is available, the model should judge the menu.

### Prompt Goal

The prompt should tell the model:

- these are the dietary constraints for the group
- this is the menu evidence
- judge restaurant-level suitability only
- do not invent menu items or dietary facts
- be conservative when the evidence is weak

### Expected Status Values

The model should return one of:

- `PASS`
- `FAIL`
- `UNSURE`

### Meaning Of Each Status

- `PASS`
  - the menu appears to provide meaningful options for the dietary needs
- `FAIL`
  - the menu appears clearly unsuitable for one or more dietary needs
- `UNSURE`
  - the menu evidence is too thin or ambiguous to judge confidently

## Suggested Output Shape

The first cut should keep the model output small.

Suggested fields:

- `status`
- `rationale`

Example:

```json
{
  "status": "PASS",
  "rationale": "The menu includes several vegetarian dishes and clearly labeled plant-based options."
}
```

## Suggested Service Design

The first cut can keep this as one class.

Suggested package:

`com.example.jarvis.constraintchecking.dietary`

Suggested class:

- `DietarySuitabilityCheck`

Suggested dependencies:

- `MenuService`
- `ChatClient`

If needed later, the evidence-building logic can be extracted into a helper such as:

- `DietaryEvidenceBuilder`

But that is not necessary for the first workshop cut.

## Suggested Implementation Shape

```java
package com.example.jarvis.constraintchecking.dietary;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.restaurant.MenuService;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class DietarySuitabilityCheck {

  private final MenuService menuService;
  private final ChatClient chatClient;

  public DietarySuitabilityCheck(MenuService menuService, ChatClient.Builder chatClientBuilder) {
    this.menuService = menuService;
    this.chatClient = chatClientBuilder.build();
  }

  public DietarySuitabilityResult check(
      List<DietaryConstraint> dietaryConstraints, String restaurantId) {

    if (restaurantId == null || restaurantId.isBlank()) {
      throw new IllegalArgumentException("restaurantId must be provided");
    }

    var normalizedConstraints =
        dietaryConstraints == null
            ? List.<DietaryConstraint>of()
            : dietaryConstraints.stream()
                .filter(constraint -> constraint != null && constraint != DietaryConstraint.NONE)
                .distinct()
                .toList();

    if (normalizedConstraints.isEmpty()) {
      return new DietarySuitabilityResult(
          DietarySuitabilityStatus.PASS, "No dietary constraints need to be checked.");
    }

    var menu = menuService.findById(restaurantId);
    if (menu.isEmpty()) {
      return new DietarySuitabilityResult(
          DietarySuitabilityStatus.UNSURE, "No menu data is available for this restaurant.");
    }

    return chatClient
        .prompt()
        .system(
            """
            You are judging whether a restaurant menu appears suitable for a group's dietary needs.

            Rules:
            - Judge the restaurant overall, not each item individually.
            - Use only the dietary constraints and menu evidence provided.
            - Do not invent menu items or dietary accommodations.
            - Return PASS, FAIL, or UNSURE.
            - Be conservative when evidence is weak or ambiguous.
            """)
        .user(
            u ->
                u.text(
                        """
                    <dietaryConstraints>
                    {dietaryConstraints}
                    </dietaryConstraints>

                    <menuJson>
                    {menuJson}
                    </menuJson>
                    """)
                    .param("dietaryConstraints", JsonUtils.toJson(normalizedConstraints))
                    .param("menuJson", JsonUtils.toJson(menu.get())))
        .call()
        .entity(DietarySuitabilityResult.class);
  }
}
```

## Suggested Supporting Types

The check can use small local types such as:

```java
public enum DietarySuitabilityStatus {
  PASS,
  FAIL,
  UNSURE
}
```

```java
public record DietarySuitabilityResult(
    DietarySuitabilityStatus status,
    String rationale) {}
```

## Suggested Unit And Integration Test Strategy

For this workshop, this check should be covered with integration tests only.

That keeps the codebase simpler to navigate and focuses the test story on the actual hybrid
behavior:

- real `MenuService`
- real fake menu JSON
- real model

### Integration Tests

Good scenario coverage:

- throws when `restaurantId` is null or blank
- returns `PASS` when there are no dietary constraints
- returns `UNSURE` when menu data is missing
- vegetarian-friendly menu -> expect `PASS`
- meat-centric menu with limited vegetarian options -> expect `FAIL` or `UNSURE`

### Suggested Integration Test Shape

```java
package com.example.jarvis.constraintchecking.dietary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jarvis.requirements.DietaryConstraint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
class DietarySuitabilityCheckIntegrationTest {

  @Autowired private DietarySuitabilityCheck check;

  @Test
  @DisplayName("Returns PASS when there are no dietary constraints")
  void returnsPassWhenThereAreNoDietaryConstraints() {
    var result = check.check(List.of(DietaryConstraint.NONE), "canoe");

    assertThat(result.status()).isEqualTo(DietarySuitabilityStatus.PASS);
    assertThat(result.rationale()).isEqualTo("No dietary constraints need to be checked.");
  }

  @Test
  @DisplayName("Returns UNSURE when menu data is missing")
  void returnsUnsureWhenMenuDataIsMissing() {
    var result = check.check(List.of(DietaryConstraint.VEGETARIAN), "does-not-exist");

    assertThat(result.status()).isEqualTo(DietarySuitabilityStatus.UNSURE);
    assertThat(result.rationale()).isEqualTo("No menu data is available for this restaurant.");
  }

  @Test
  @DisplayName("Throws when restaurant id is blank")
  void throwsWhenRestaurantIdIsBlank() {
    assertThatThrownBy(() -> check.check(List.of(DietaryConstraint.VEGETARIAN), " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantId must be provided");
  }

  @Test
  @DisplayName("Planta Queen is a strong vegetarian fit")
  void plantaQueenIsAStrongVegetarianFit() {
    var result = check.check(List.of(DietaryConstraint.VEGETARIAN), "planta-queen");

    assertThat(result.status()).isEqualTo(DietarySuitabilityStatus.PASS);
  }

  @Test
  @DisplayName("Kintori Yakitori is not a strong vegetarian fit")
  void kintoriYakitoriIsNotAStrongVegetarianFit() {
    var result = check.check(List.of(DietaryConstraint.VEGETARIAN), "kintori-yakitori");

    assertThat(result.status()).isIn(
        DietarySuitabilityStatus.FAIL,
        DietarySuitabilityStatus.UNSURE);
  }
}
```

## Why This Works Well In A Workshop

This design teaches:

- how to separate evidence gathering from judgment
- how to use deterministic code to prepare evidence for an LLM
- how to short-circuit unnecessary model calls
- how to keep the LLM's task narrow and inspectable

It also uses fake data the project already has:

- attendee dietary constraints from Phase 1
- restaurant menus from `components/restaurant-data`
