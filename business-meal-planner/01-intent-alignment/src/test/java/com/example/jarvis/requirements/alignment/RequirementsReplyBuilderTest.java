package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.agent.RequirementStatus;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.jarvis.requirements.EventRequirements;
import com.example.jarvis.requirements.MealType;
import com.example.jarvis.requirements.TravelMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementsReplyBuilderTest {

  private final RequirementsReplyBuilder replyBuilder = new RequirementsReplyBuilder();

  @Test
  void buildsClarificationReplyFromFirstMissingField() {
    JarvisAgentContext context = new JarvisAgentContext();
    context.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
    context.setMissingInformation(List.of("Time", "Party Size"));

    assertThat(replyBuilder.buildReply(context)).contains("what is the time?");
  }

  @Test
  void buildsConfirmationReplyFromCapturedContext() {
    EventRequirements requirements = new EventRequirements();
    requirements.setDate(LocalDate.of(2026, 4, 11));
    requirements.setTime(LocalTime.of(19, 0));
    requirements.setPartySize(4);
    requirements.setMealType(MealType.DINNER);
    requirements.setPurpose("Client dinner");

    Attendee attendee = new Attendee();
    attendee.setName("Alex");
    attendee.setOrigin("Union Station");
    attendee.setDepartureTime(LocalTime.of(18, 15));
    attendee.setTravelMode(TravelMode.TRANSIT);
    attendee.setDietaryConstraints(List.of(DietaryConstraint.VEGETARIAN));

    JarvisAgentContext context = new JarvisAgentContext();
    context.setEventRequirements(requirements);
    context.setAttendees(List.of(attendee));
    context.setStatus(RequirementStatus.WAITING_FOR_CONFIRMATION);

    String reply = replyBuilder.buildReply(context);

    assertThat(reply).contains("Here's my understanding so far:");
    assertThat(reply).contains("- Date: 2026-04-11");
    assertThat(reply).contains("- Meal Type: dinner");
    assertThat(reply).contains("Alex");
    assertThat(reply).contains("dietary: vegetarian");
    assertThat(reply).contains("Please confirm or correct");
  }

  @Test
  void buildsConfirmedReply() {
    JarvisAgentContext context = new JarvisAgentContext();
    context.setStatus(RequirementStatus.REQUIREMENTS_CONFIRMED);

    assertThat(replyBuilder.buildReply(context)).contains("requirements and they're confirmed");
  }
}
