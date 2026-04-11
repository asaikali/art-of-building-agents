package com.example.jarvis.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.MealType;
import com.example.jarvis.requirements.NoiseLevel;
import com.example.jarvis.requirements.TravelMode;
import com.example.jarvis.requirements.UserRequirements;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class JarvisAgentContextMarkdownTest {

  @Test
  void rendersRequiredSectionsAndStatusLabel() {
    JarvisAgentContext state = new JarvisAgentContext();
    UserRequirements userRequirements = new UserRequirements();
    userRequirements.setMeal(
        meal(
            LocalDate.of(2026, 4, 11),
            LocalTime.of(18, 0),
            4,
            MealType.DINNER,
            "Client dinner",
            new BigDecimal("120"),
            NoiseLevel.QUIET,
            List.of("Professional setting"),
            List.of("Italian")));
    userRequirements.setAttendees(List.of(attendee("Alex", "Union Station", TravelMode.TRANSIT)));
    state.setUserRequirements(userRequirements);
    state.setMissingInformation(List.of());
    state.setStatus(RequirementStatus.WAITING_FOR_CONFIRMATION);

    String markdown = state.toMarkdown();

    assertThat(markdown).contains("## Meal");
    assertThat(markdown).contains("Date: 2026-04-11");
    assertThat(markdown).contains("## Additional Requirements");
    assertThat(markdown).contains("## Cuisine Preferences");
    assertThat(markdown).contains("## Attendees");
    assertThat(markdown).contains("Alex");
    assertThat(markdown).contains("## Missing Information");
    assertThat(markdown).contains("## Status");
    assertThat(markdown).contains("Waiting for confirmation");
  }

  @Test
  void rendersNoneForEmptyLists() {
    JarvisAgentContext state = new JarvisAgentContext();
    UserRequirements userRequirements = new UserRequirements();
    userRequirements.setMeal(new Meal());
    userRequirements.setAttendees(List.of());
    state.setUserRequirements(userRequirements);
    state.setMissingInformation(List.of());
    state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);

    String markdown = state.toMarkdown();

    assertThat(markdown).contains("Date: Missing");
    assertThat(markdown).contains("## Additional Requirements\n- None");
    assertThat(markdown).contains("## Cuisine Preferences\n- None");
    assertThat(markdown).contains("## Attendees\n- None");
    assertThat(markdown).contains("## Missing Information\n- None");
    assertThat(markdown).contains("Waiting for clarification");
  }

  private Meal meal(
      LocalDate date,
      LocalTime time,
      Integer partySize,
      MealType mealType,
      String purpose,
      BigDecimal budgetPerPerson,
      NoiseLevel noiseLevel,
      List<String> additionalRequirements,
      List<String> cuisinePreferences) {
    Meal meal = new Meal();
    meal.setDate(date);
    meal.setTime(time);
    meal.setPartySize(partySize);
    meal.setMealType(mealType);
    meal.setPurpose(purpose);
    meal.setBudgetPerPerson(budgetPerPerson);
    meal.setNoiseLevel(noiseLevel);
    meal.setAdditionalRequirements(additionalRequirements);
    meal.setCuisinePreferences(cuisinePreferences);
    return meal;
  }

  private Attendee attendee(String name, String origin, TravelMode travelMode) {
    Attendee attendee = new Attendee();
    attendee.setName(name);
    attendee.setOrigin(origin);
    attendee.setDepartureTime(LocalTime.of(18, 15));
    attendee.setTravelMode(travelMode);
    attendee.setMaxTravelTimeMinutes(30);
    attendee.setMaxDistanceKm(10.0);
    attendee.setDietaryConstraints(List.of(DietaryConstraint.VEGETARIAN));
    return attendee;
  }
}
