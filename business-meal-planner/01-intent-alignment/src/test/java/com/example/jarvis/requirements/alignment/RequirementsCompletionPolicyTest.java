package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.agent.RequirementStatus;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.UserRequirements;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class RequirementsCompletionPolicyTest {

  private final RequirementsCompletionPolicy completionPolicy = new RequirementsCompletionPolicy();

  @Test
  void returnsMissingCriticalFields() {
    Meal meal = new Meal();
    meal.setDate(LocalDate.of(2026, 4, 11));

    assertThat(completionPolicy.missingCriticalFields(meal)).containsExactly("Time", "Party Size");
  }

  @Test
  void waitsForClarificationWhenFieldsAreMissing() {
    JarvisAgentContext context = new JarvisAgentContext();
    context.setUserRequirements(new UserRequirements());
    context.setMissingInformation(java.util.List.of("Time"));

    assertThat(completionPolicy.decideStatus(context))
        .isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
  }

  @Test
  void waitsForConfirmationWhenCriticalFieldsArePresent() {
    Meal meal = new Meal();
    meal.setDate(LocalDate.of(2026, 4, 11));
    meal.setTime(LocalTime.of(19, 0));
    meal.setPartySize(4);

    UserRequirements userRequirements = new UserRequirements();
    userRequirements.setMeal(meal);

    JarvisAgentContext context = new JarvisAgentContext();
    context.setUserRequirements(userRequirements);
    context.setMissingInformation(completionPolicy.missingCriticalFields(meal));

    assertThat(completionPolicy.decideStatus(context))
        .isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
  }
}
