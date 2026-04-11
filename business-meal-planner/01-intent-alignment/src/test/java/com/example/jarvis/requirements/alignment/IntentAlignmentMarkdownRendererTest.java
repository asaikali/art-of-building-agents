package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.jarvis.requirements.EventRequirements;
import com.example.jarvis.requirements.MealType;
import com.example.jarvis.requirements.NoiseLevel;
import com.example.jarvis.requirements.TravelMode;
import com.example.jarvis.state.AgentState;
import com.example.jarvis.state.RequirementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntentAlignmentMarkdownRendererTest {

  private final IntentAlignmentMarkdownRenderer renderer = new IntentAlignmentMarkdownRenderer();

  @Test
  void rendersRequiredSectionsAndStatusLabel() {
    AgentState state = new AgentState();
    state.setEventRequirements(
        eventRequirements(
            LocalDate.of(2026, 4, 11),
            LocalTime.of(18, 0),
            4,
            MealType.DINNER,
            "Client dinner",
            new BigDecimal("120"),
            NoiseLevel.QUIET,
            List.of("Professional setting"),
            List.of("Italian")));
    state.setAttendees(List.of(attendee("Alex", "Union Station", TravelMode.TRANSIT)));
    state.setMissingInformation(List.of());
    state.setStatus(RequirementStatus.WAITING_FOR_CONFIRMATION);

    String markdown = renderer.render(state);

    assertThat(markdown).contains("## Event Requirements");
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
    AgentState state = new AgentState();
    state.setEventRequirements(new EventRequirements());
    state.setAttendees(List.of());
    state.setMissingInformation(List.of());
    state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);

    String markdown = renderer.render(state);

    assertThat(markdown).contains("Date: Missing");
    assertThat(markdown).contains("## Additional Requirements\n- None");
    assertThat(markdown).contains("## Cuisine Preferences\n- None");
    assertThat(markdown).contains("## Attendees\n- None");
    assertThat(markdown).contains("## Missing Information\n- None");
    assertThat(markdown).contains("Waiting for clarification");
  }

  private EventRequirements eventRequirements(
      LocalDate date,
      LocalTime time,
      Integer partySize,
      MealType mealType,
      String purpose,
      BigDecimal budgetPerPerson,
      NoiseLevel noiseLevel,
      List<String> additionalRequirements,
      List<String> cuisinePreferences) {
    EventRequirements eventRequirements = new EventRequirements();
    eventRequirements.setDate(date);
    eventRequirements.setTime(time);
    eventRequirements.setPartySize(partySize);
    eventRequirements.setMealType(mealType);
    eventRequirements.setPurpose(purpose);
    eventRequirements.setBudgetPerPerson(budgetPerPerson);
    eventRequirements.setNoiseLevel(noiseLevel);
    eventRequirements.setAdditionalRequirements(additionalRequirements);
    eventRequirements.setCuisinePreferences(cuisinePreferences);
    return eventRequirements;
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
