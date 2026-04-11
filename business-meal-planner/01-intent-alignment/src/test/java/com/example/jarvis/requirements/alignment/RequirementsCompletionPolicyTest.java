package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.agent.RequirementStatus;
import com.example.jarvis.requirements.EventRequirements;
import com.example.jarvis.requirements.UserRequirements;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class RequirementsCompletionPolicyTest {

  private final RequirementsCompletionPolicy completionPolicy = new RequirementsCompletionPolicy();

  @Test
  void returnsMissingCriticalFields() {
    EventRequirements requirements = new EventRequirements();
    requirements.setDate(LocalDate.of(2026, 4, 11));

    assertThat(completionPolicy.missingCriticalFields(requirements))
        .containsExactly("Time", "Party Size");
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
    EventRequirements requirements = new EventRequirements();
    requirements.setDate(LocalDate.of(2026, 4, 11));
    requirements.setTime(LocalTime.of(19, 0));
    requirements.setPartySize(4);

    UserRequirements userRequirements = new UserRequirements();
    userRequirements.setEventRequirements(requirements);

    JarvisAgentContext context = new JarvisAgentContext();
    context.setUserRequirements(userRequirements);
    context.setMissingInformation(completionPolicy.missingCriticalFields(requirements));

    assertThat(completionPolicy.decideStatus(context))
        .isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
  }
}
