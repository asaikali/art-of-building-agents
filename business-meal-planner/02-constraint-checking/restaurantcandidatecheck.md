# Restaurant Candidate Check Service

This document describes the design of a higher-level service that runs all Phase 2 checks
against a single `RestaurantCandidate`.

The goal is to provide one strongly typed aggregate result without introducing Phase 3 concerns
such as:

- hard vs soft classification
- scoring
- ranking
- filtering

## Scope

`RestaurantCandidateCheckService` answers a simple question:

> Given confirmed user requirements and one restaurant candidate, what did each Phase 2 check
> say about that candidate?

This service is an orchestration layer.

It should:

- invoke the underlying checks
- adapt `UserRequirements` into the narrower inputs required by those checks
- return a strongly typed aggregate result

It should not:

- score the candidate
- decide whether a result is hard or soft
- decide whether the candidate should be rejected

Those responsibilities belong to a higher layer such as Phase 3 restaurant planning.

## Why This Service Exists

The individual checks have deliberately small input shapes.

Examples:

- `NoiseLevelCheck` only needs requested `NoiseLevel` and `restaurantId`
- `BudgetPerPersonCheck` only needs requested budget and `restaurantId`
- `DietarySuitabilityCheck` only needs `List<DietaryConstraint>` and `restaurantId`

But the caller often starts from:

- `UserRequirements`
- `RestaurantCandidate`

This service acts as the adapter between those higher-level inputs and the smaller check APIs.

## Suggested Service Design

Suggested package:

`com.example.jarvis.constraintchecking`

Suggested class:

- `RestaurantCandidateCheckService`

Suggested dependencies:

- `NoiseLevelCheck`
- `BudgetPerPersonCheck`
- `TravelTimeCheck`
- `DietarySuitabilityCheck`
- `BusinessMealSuitabilityCheck`

## Suggested Candidate Shape

The first cut can keep `RestaurantCandidate` minimal:

```java
package com.example.jarvis.constraintchecking;

public record RestaurantCandidate(
    String restaurantId,
    String name) {}
```

## Strongly Typed Aggregate Result

Instead of returning a generic list of objects, the service should return a strongly typed result
that contains one field per underlying check.

Suggested shape:

```java
package com.example.jarvis.constraintchecking;

import com.example.jarvis.constraintchecking.budget.BudgetPerPersonCheckResult;
import com.example.jarvis.constraintchecking.businessmeal.BusinessMealSuitabilityResult;
import com.example.jarvis.constraintchecking.dietary.DietarySuitabilityResult;
import com.example.jarvis.constraintchecking.noise.NoiseLevelCheckResult;
import com.example.jarvis.constraintchecking.travel.TravelTimeCheckResult;

public record RestaurantCheckResult(
    RestaurantCandidate candidate,
    NoiseLevelCheckResult noiseLevel,
    BudgetPerPersonCheckResult budgetPerPerson,
    TravelTimeCheckResult travelTime,
    DietarySuitabilityResult dietarySuitability,
    BusinessMealSuitabilityResult businessMealSuitability) {}
```

This keeps Phase 2 strongly typed while still leaving interpretation decisions to the caller.

## Suggested Implementation Shape

```java
package com.example.jarvis.constraintchecking;

import com.example.jarvis.constraintchecking.budget.BudgetPerPersonCheck;
import com.example.jarvis.constraintchecking.businessmeal.BusinessMealSuitabilityCheck;
import com.example.jarvis.constraintchecking.dietary.DietarySuitabilityCheck;
import com.example.jarvis.constraintchecking.noise.NoiseLevelCheck;
import com.example.jarvis.constraintchecking.travel.TravelTimeCheck;
import com.example.jarvis.requirements.UserRequirements;
import org.springframework.stereotype.Service;

@Service
public class RestaurantCandidateCheckService {

  private final NoiseLevelCheck noiseLevelCheck;
  private final BudgetPerPersonCheck budgetPerPersonCheck;
  private final TravelTimeCheck travelTimeCheck;
  private final DietarySuitabilityCheck dietarySuitabilityCheck;
  private final BusinessMealSuitabilityCheck businessMealSuitabilityCheck;

  public RestaurantCandidateCheckService(
      NoiseLevelCheck noiseLevelCheck,
      BudgetPerPersonCheck budgetPerPersonCheck,
      TravelTimeCheck travelTimeCheck,
      DietarySuitabilityCheck dietarySuitabilityCheck,
      BusinessMealSuitabilityCheck businessMealSuitabilityCheck) {
    this.noiseLevelCheck = noiseLevelCheck;
    this.budgetPerPersonCheck = budgetPerPersonCheck;
    this.travelTimeCheck = travelTimeCheck;
    this.dietarySuitabilityCheck = dietarySuitabilityCheck;
    this.businessMealSuitabilityCheck = businessMealSuitabilityCheck;
  }

  public RestaurantCheckResult check(UserRequirements requirements, RestaurantCandidate candidate) {
    var meal = requirements.getMeal();

    return new RestaurantCheckResult(
        candidate,
        noiseLevelCheck.check(meal.getNoiseLevel(), candidate.restaurantId()),
        budgetPerPersonCheck.check(meal.getBudgetPerPerson(), candidate.restaurantId()),
        travelTimeCheck.check(requirements, candidate),
        dietarySuitabilityCheck.check(
            requirements.getAttendees().stream()
                .flatMap(attendee -> attendee.getDietaryConstraints().stream())
                .toList(),
            candidate.restaurantId()),
        businessMealSuitabilityCheck.check(meal, candidate.restaurantId()));
  }
}
```

## Why The Dietary Flattening Lives Here

`DietarySuitabilityCheck` intentionally does not take `UserRequirements`.

It only takes:

- `List<DietaryConstraint>`
- `restaurantId`

That keeps the lower-level dietary checker decoupled from the higher-level `UserRequirements`
aggregate.

Because of that design, this service must adapt:

- `List<Attendee>` with per-attendee constraints

into:

- one flat `List<DietaryConstraint>`

That flattening is not accidental. It is the real adapter responsibility of this layer.

## Suggested Test Shape

Unlike the individual checks, this service is a good place to use mocks.

The service itself is not testing:

- pricing logic
- noise logic
- travel logic
- dietary prompt behavior
- LLM judgment

Its responsibility is orchestration and adaptation.

Suggested test shape:

```java
package com.example.jarvis.constraintchecking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.jarvis.constraintchecking.budget.BudgetPerPersonCheck;
import com.example.jarvis.constraintchecking.budget.BudgetPerPersonCheckResult;
import com.example.jarvis.constraintchecking.budget.BudgetPerPersonCheckStatus;
import com.example.jarvis.constraintchecking.businessmeal.BusinessMealSuitabilityCheck;
import com.example.jarvis.constraintchecking.businessmeal.BusinessMealSuitabilityResult;
import com.example.jarvis.constraintchecking.businessmeal.BusinessMealSuitabilityStatus;
import com.example.jarvis.constraintchecking.dietary.DietarySuitabilityCheck;
import com.example.jarvis.constraintchecking.dietary.DietarySuitabilityResult;
import com.example.jarvis.constraintchecking.dietary.DietarySuitabilityStatus;
import com.example.jarvis.constraintchecking.noise.NoiseLevelCheck;
import com.example.jarvis.constraintchecking.noise.NoiseLevelCheckResult;
import com.example.jarvis.constraintchecking.noise.NoiseLevelCheckStatus;
import com.example.jarvis.constraintchecking.travel.TravelTimeCheck;
import com.example.jarvis.constraintchecking.travel.TravelTimeCheckResult;
import com.example.jarvis.constraintchecking.travel.TravelTimeCheckStatus;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.NoiseLevel;
import com.example.jarvis.requirements.UserRequirements;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RestaurantCandidateCheckServiceTest {

  private NoiseLevelCheck noiseLevelCheck;
  private BudgetPerPersonCheck budgetPerPersonCheck;
  private TravelTimeCheck travelTimeCheck;
  private DietarySuitabilityCheck dietarySuitabilityCheck;
  private BusinessMealSuitabilityCheck businessMealSuitabilityCheck;

  private RestaurantCandidateCheckService service;

  @BeforeEach
  void setUp() {
    noiseLevelCheck = Mockito.mock(NoiseLevelCheck.class);
    budgetPerPersonCheck = Mockito.mock(BudgetPerPersonCheck.class);
    travelTimeCheck = Mockito.mock(TravelTimeCheck.class);
    dietarySuitabilityCheck = Mockito.mock(DietarySuitabilityCheck.class);
    businessMealSuitabilityCheck = Mockito.mock(BusinessMealSuitabilityCheck.class);

    service =
        new RestaurantCandidateCheckService(
            noiseLevelCheck,
            budgetPerPersonCheck,
            travelTimeCheck,
            dietarySuitabilityCheck,
            businessMealSuitabilityCheck);
  }

  @Test
  @DisplayName("Aggregates all underlying check results into one typed result")
  void aggregatesAllUnderlyingCheckResults() {
    var requirements = new UserRequirements();

    var meal = new Meal();
    meal.setNoiseLevel(NoiseLevel.QUIET);
    meal.setBudgetPerPerson(new BigDecimal("80"));
    meal.setPurpose("client dinner");
    requirements.setMeal(meal);

    var attendee = new Attendee();
    attendee.setDietaryConstraints(
        List.of(DietaryConstraint.VEGETARIAN, DietaryConstraint.GLUTEN_FREE));
    requirements.setAttendees(List.of(attendee));

    var candidate = new RestaurantCandidate("canoe", "Canoe Restaurant");

    var noiseResult =
        new NoiseLevelCheckResult(NoiseLevelCheckStatus.PASS, "Requested QUIET, restaurant is QUIET.");
    var budgetResult =
        new BudgetPerPersonCheckResult(
            BudgetPerPersonCheckStatus.MAYBE,
            "Budget 80 falls inside the restaurant range 70-110 CAD.");
    var travelResult =
        new TravelTimeCheckResult(
            TravelTimeCheckStatus.PASS,
            14,
            "Estimated travel time is 14 minutes.");
    var dietaryResult =
        new DietarySuitabilityResult(
            DietarySuitabilityStatus.PASS,
            "The menu appears to provide meaningful vegetarian and gluten-free options.");
    var businessMealResult =
        new BusinessMealSuitabilityResult(
            BusinessMealSuitabilityStatus.PASS,
            "The restaurant appears suitable for a client dinner.");

    when(noiseLevelCheck.check(NoiseLevel.QUIET, "canoe")).thenReturn(noiseResult);
    when(budgetPerPersonCheck.check(new BigDecimal("80"), "canoe")).thenReturn(budgetResult);
    when(travelTimeCheck.check(requirements, candidate)).thenReturn(travelResult);
    when(dietarySuitabilityCheck.check(
            List.of(DietaryConstraint.VEGETARIAN, DietaryConstraint.GLUTEN_FREE), "canoe"))
        .thenReturn(dietaryResult);
    when(businessMealSuitabilityCheck.check(meal, "canoe")).thenReturn(businessMealResult);

    var result = service.check(requirements, candidate);

    assertThat(result.candidate()).isEqualTo(candidate);
    assertThat(result.noiseLevel()).isEqualTo(noiseResult);
    assertThat(result.budgetPerPerson()).isEqualTo(budgetResult);
    assertThat(result.travelTime()).isEqualTo(travelResult);
    assertThat(result.dietarySuitability()).isEqualTo(dietaryResult);
    assertThat(result.businessMealSuitability()).isEqualTo(businessMealResult);
  }

  @Test
  @DisplayName("Flattens attendee dietary constraints before invoking the dietary check")
  void flattensAttendeeDietaryConstraints() {
    var requirements = new UserRequirements();

    var meal = new Meal();
    requirements.setMeal(meal);

    var attendee1 = new Attendee();
    attendee1.setDietaryConstraints(List.of(DietaryConstraint.VEGETARIAN));

    var attendee2 = new Attendee();
    attendee2.setDietaryConstraints(List.of(DietaryConstraint.GLUTEN_FREE));

    requirements.setAttendees(List.of(attendee1, attendee2));

    var candidate = new RestaurantCandidate("canoe", "Canoe Restaurant");

    when(noiseLevelCheck.check(null, "canoe"))
        .thenReturn(new NoiseLevelCheckResult(NoiseLevelCheckStatus.PASS, "No preference."));
    when(budgetPerPersonCheck.check(null, "canoe"))
        .thenReturn(new BudgetPerPersonCheckResult(BudgetPerPersonCheckStatus.PASS, "No budget."));
    when(travelTimeCheck.check(requirements, candidate))
        .thenReturn(new TravelTimeCheckResult(TravelTimeCheckStatus.PASS, 10, "Estimated 10."));
    when(dietarySuitabilityCheck.check(
            List.of(DietaryConstraint.VEGETARIAN, DietaryConstraint.GLUTEN_FREE), "canoe"))
        .thenReturn(
            new DietarySuitabilityResult(
                DietarySuitabilityStatus.UNSURE, "Menu evidence is ambiguous."));
    when(businessMealSuitabilityCheck.check(meal, "canoe"))
        .thenReturn(
            new BusinessMealSuitabilityResult(
                BusinessMealSuitabilityStatus.UNSURE, "Limited context."));

    service.check(requirements, candidate);

    // This assertion exists because flattening attendee dietary constraints is real adapter
    // logic owned by this service. DietarySuitabilityCheck deliberately accepts only the lower-
    // level List<DietaryConstraint> input so it does not depend on UserRequirements.
    verify(dietarySuitabilityCheck)
        .check(List.of(DietaryConstraint.VEGETARIAN, DietaryConstraint.GLUTEN_FREE), "canoe");
  }
}
```

## Why This Works Well In A Workshop

This service teaches:

- how to compose several narrow checks into one candidate-level report
- how to keep lower-level checks decoupled from `UserRequirements`
- how to make adapter logic explicit
- how to return a strongly typed aggregate without prematurely introducing scoring

It is a good bridge between Phase 2 checking and Phase 3 planning.
