package com.example.jarvis.constraints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.jarvis.constraints.deterministic.budget.BudgetPerPersonCheck;
import com.example.jarvis.constraints.deterministic.budget.BudgetPerPersonCheckResult;
import com.example.jarvis.constraints.deterministic.budget.BudgetPerPersonCheckStatus;
import com.example.jarvis.constraints.deterministic.noise.NoiseLevelCheck;
import com.example.jarvis.constraints.deterministic.noise.NoiseLevelCheckResult;
import com.example.jarvis.constraints.deterministic.noise.NoiseLevelCheckStatus;
import com.example.jarvis.constraints.deterministic.travel.TravelTimeCheck;
import com.example.jarvis.constraints.deterministic.travel.TravelTimeCheckResult;
import com.example.jarvis.constraints.deterministic.travel.TravelTimeCheckStatus;
import com.example.jarvis.constraints.hybrid.dietary.DietarySuitabilityCheck;
import com.example.jarvis.constraints.hybrid.dietary.DietarySuitabilityResult;
import com.example.jarvis.constraints.hybrid.dietary.DietarySuitabilityStatus;
import com.example.jarvis.constraints.llmjudge.suitability.MealSuitabilityCheck;
import com.example.jarvis.constraints.llmjudge.suitability.MealSuitabilityResult;
import com.example.jarvis.constraints.llmjudge.suitability.MealSuitabilityStatus;
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
  private MealSuitabilityCheck mealSuitabilityCheck;

  private RestaurantCandidateCheckService service;

  @BeforeEach
  void setUp() {
    noiseLevelCheck = Mockito.mock(NoiseLevelCheck.class);
    budgetPerPersonCheck = Mockito.mock(BudgetPerPersonCheck.class);
    travelTimeCheck = Mockito.mock(TravelTimeCheck.class);
    dietarySuitabilityCheck = Mockito.mock(DietarySuitabilityCheck.class);
    mealSuitabilityCheck = Mockito.mock(MealSuitabilityCheck.class);

    service =
        new RestaurantCandidateCheckService(
            noiseLevelCheck,
            budgetPerPersonCheck,
            travelTimeCheck,
            dietarySuitabilityCheck,
            mealSuitabilityCheck);
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
        new NoiseLevelCheckResult(
            NoiseLevelCheckStatus.PASS, "Requested QUIET, restaurant is QUIET.");
    var budgetResult =
        new BudgetPerPersonCheckResult(
            BudgetPerPersonCheckStatus.MAYBE, "Budget 80 falls inside range 70-110 CAD.");
    var travelResult =
        new TravelTimeCheckResult(
            TravelTimeCheckStatus.UNSURE, "Required travel inputs are missing.", 0);
    var dietaryResult =
        new DietarySuitabilityResult(DietarySuitabilityStatus.PASS, "Menu has vegetarian options.");
    var mealResult =
        new MealSuitabilityResult(MealSuitabilityStatus.PASS, "Suitable for client dinner.");

    when(noiseLevelCheck.check(NoiseLevel.QUIET, "canoe")).thenReturn(noiseResult);
    when(budgetPerPersonCheck.check(new BigDecimal("80"), "canoe")).thenReturn(budgetResult);
    when(travelTimeCheck.check(null, null, null, "canoe")).thenReturn(travelResult);
    when(dietarySuitabilityCheck.check(
            List.of(DietaryConstraint.VEGETARIAN, DietaryConstraint.GLUTEN_FREE), "canoe"))
        .thenReturn(dietaryResult);
    when(mealSuitabilityCheck.check(meal, "canoe")).thenReturn(mealResult);

    var result = service.check(requirements, candidate);

    assertThat(result.candidate()).isEqualTo(candidate);
    assertThat(result.noiseLevel()).isEqualTo(noiseResult);
    assertThat(result.budgetPerPerson()).isEqualTo(budgetResult);
    assertThat(result.travelTime()).isEqualTo(travelResult);
    assertThat(result.dietarySuitability()).isEqualTo(dietaryResult);
    assertThat(result.mealSuitability()).isEqualTo(mealResult);
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
    when(travelTimeCheck.check(null, null, null, "canoe"))
        .thenReturn(new TravelTimeCheckResult(TravelTimeCheckStatus.UNSURE, "Missing inputs.", 0));
    when(dietarySuitabilityCheck.check(
            List.of(DietaryConstraint.VEGETARIAN, DietaryConstraint.GLUTEN_FREE), "canoe"))
        .thenReturn(
            new DietarySuitabilityResult(DietarySuitabilityStatus.UNSURE, "Ambiguous evidence."));
    when(mealSuitabilityCheck.check(meal, "canoe"))
        .thenReturn(new MealSuitabilityResult(MealSuitabilityStatus.UNSURE, "Limited context."));

    service.check(requirements, candidate);

    verify(dietarySuitabilityCheck)
        .check(List.of(DietaryConstraint.VEGETARIAN, DietaryConstraint.GLUTEN_FREE), "canoe");
  }
}
