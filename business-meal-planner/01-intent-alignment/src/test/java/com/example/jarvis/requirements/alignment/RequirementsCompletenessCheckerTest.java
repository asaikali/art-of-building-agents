package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.MealType;
import com.example.jarvis.requirements.UserRequirements;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementsCompletenessCheckerTest {

  private final RequirementsCompletenessChecker checker = new RequirementsCompletenessChecker();

  @Test
  void returnsMissingCriticalFields() {
    Meal meal = new Meal();
    meal.setDate(LocalDate.of(2026, 4, 11));

    assertThat(checker.missingCriticalFields(meal)).containsExactly("Time", "Party Size");
  }

  @Test
  void waitsForClarificationWhenFieldsAreMissing() {
    assertThat(checker.decideStatus(List.of("Time"), false))
        .isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
  }

  @Test
  void waitsForConfirmationWhenCriticalFieldsArePresent() {
    assertThat(checker.decideStatus(List.of(), false))
        .isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
  }

  @Test
  void confirmsWhenUserConfirmedAndNoCriticalFieldsMissing() {
    assertThat(checker.decideStatus(List.of(), true))
        .isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
  }

  @Test
  void clarificationOverridesConfirmationWhenFieldsStillMissing() {
    assertThat(checker.decideStatus(List.of("Date"), true))
        .isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
  }

  @Test
  void suggestsDietaryForLargeGroupWithoutConstraints() {
    UserRequirements requirements = requirements(meal(4, MealType.LUNCH, null), List.of());

    assertThat(checker.suggestFollowUps(requirements)).anyMatch(s -> s.contains("dietary"));
  }

  @Test
  void doesNotSuggestDietaryWhenConstraintsPresent() {
    Attendee attendee = new Attendee();
    attendee.setName("Alex");
    attendee.setDietaryConstraints(List.of(DietaryConstraint.VEGETARIAN));
    UserRequirements requirements = requirements(meal(4, MealType.LUNCH, null), List.of(attendee));

    assertThat(checker.suggestFollowUps(requirements)).noneMatch(s -> s.contains("dietary"));
  }

  @Test
  void suggestsBudgetForClientDinner() {
    UserRequirements requirements =
        requirements(meal(4, MealType.DINNER, "Client dinner"), List.of());

    assertThat(checker.suggestFollowUps(requirements)).anyMatch(s -> s.contains("budget"));
  }

  @Test
  void suggestsNoiseLevelWhenPurposeIsSet() {
    UserRequirements requirements = requirements(meal(4, MealType.LUNCH, "Team lunch"), List.of());

    assertThat(checker.suggestFollowUps(requirements)).anyMatch(s -> s.contains("noise"));
  }

  @Test
  void suggestsAttendeeDetailsWhenListIsEmpty() {
    UserRequirements requirements = requirements(meal(4, MealType.LUNCH, null), List.of());

    assertThat(checker.suggestFollowUps(requirements)).anyMatch(s -> s.contains("attendee"));
  }

  @Test
  void noSuggestionsWhenEverythingIsFilled() {
    Meal meal = completeMeal();
    Attendee attendee = new Attendee();
    attendee.setName("Alex");
    attendee.setDietaryConstraints(List.of(DietaryConstraint.VEGETARIAN));
    UserRequirements requirements = requirements(meal, List.of(attendee));

    assertThat(checker.suggestFollowUps(requirements)).isEmpty();
  }

  private Meal meal(int partySize, MealType mealType, String purpose) {
    Meal meal = new Meal();
    meal.setPartySize(partySize);
    meal.setMealType(mealType);
    meal.setPurpose(purpose);
    return meal;
  }

  private Meal completeMeal() {
    Meal meal = new Meal();
    meal.setDate(LocalDate.of(2026, 4, 13));
    meal.setTime(LocalTime.of(19, 0));
    meal.setPartySize(4);
    meal.setMealType(MealType.DINNER);
    meal.setPurpose("Client dinner");
    meal.setBudgetPerPerson(java.math.BigDecimal.valueOf(120));
    meal.setNoiseLevel(com.example.jarvis.requirements.NoiseLevel.QUIET);
    return meal;
  }

  private UserRequirements requirements(Meal meal, List<Attendee> attendees) {
    UserRequirements requirements = new UserRequirements();
    requirements.setMeal(meal);
    requirements.setAttendees(attendees);
    return requirements;
  }
}
