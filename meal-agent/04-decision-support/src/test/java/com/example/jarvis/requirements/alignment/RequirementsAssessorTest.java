package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jarvis.requirements.Meal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class RequirementsAssessorTest {

  private final RequirementsAssessor assessor = new RequirementsAssessor(null);

  @Test
  void findsMissingRequiredFields() {
    Meal meal = new Meal();
    meal.setDate(LocalDate.of(2026, 4, 11));

    assertThat(assessor.findMissingRequiredFields(meal)).containsExactly("Time", "Party Size");
  }

  @Test
  void returnsEmptyWhenAllRequiredFieldsPresent() {
    Meal meal = new Meal();
    meal.setDate(LocalDate.of(2026, 4, 11));
    meal.setTime(LocalTime.of(19, 0));
    meal.setPartySize(4);

    assertThat(assessor.findMissingRequiredFields(meal)).isEmpty();
  }

  @Test
  void allFieldsMissingForEmptyMeal() {
    assertThat(assessor.findMissingRequiredFields(new Meal()))
        .containsExactly("Date", "Time", "Party Size");
  }
}
