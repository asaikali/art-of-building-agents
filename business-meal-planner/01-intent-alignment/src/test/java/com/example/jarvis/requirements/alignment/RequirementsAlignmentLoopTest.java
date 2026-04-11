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
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequirementsAlignmentLoopTest {

  private FakeRequirementsExtractor requirementsExtractor;
  private RequirementsAlignmentLoop alignmentLoop;

  @BeforeEach
  void setUp() {
    requirementsExtractor = new FakeRequirementsExtractor();
    alignmentLoop = new RequirementsAlignmentLoop(requirementsExtractor);
  }

  @Test
  void initialRequestCreatesPlanAndWaitsForConfirmation() {
    JarvisAgentContext state = new JarvisAgentContext();
    requirementsExtractor.extractedContexts.add(
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 11),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Client dinner"),
            List.of(attendee("Alex", "100 King St W"))));

    RequirementsAlignmentLoop.TurnResult result =
        alignmentLoop.handleTurn(
            state, "Book a client dinner tonight at 7 PM for four people from 100 King St W.");

    assertThat(result.eventName()).isEqualTo("plan-generated");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(result.assistantReply()).contains("Please confirm or correct");
    assertThat(state.getEventRequirements()).isSameAs(result.state().getEventRequirements());
  }

  @Test
  void affirmativeReplyConfirmsExistingRequirements() {
    JarvisAgentContext state = new JarvisAgentContext();
    requirementsExtractor.extractedContexts.add(
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 12), LocalTime.of(12, 0), 6, MealType.LUNCH, "Team lunch"),
            List.of()));
    alignmentLoop.handleTurn(state, "I need a team lunch tomorrow for 6 people.");

    RequirementsAlignmentLoop.TurnResult result = alignmentLoop.handleTurn(state, "yes");

    assertThat(result.eventName()).isEqualTo("requirements-confirmed");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
    assertThat(result.assistantReply()).contains("confirmed");
  }

  @Test
  void nonActionableReplyRequestsClarificationWithoutChangingContext() {
    JarvisAgentContext state = new JarvisAgentContext();
    RequirementsExtractor.ExtractedPlanningContext initialContext =
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 11),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Business dinner"),
            List.of(attendee("Alex", "Union Station")));
    requirementsExtractor.extractedContexts.add(initialContext);
    alignmentLoop.handleTurn(state, "I need a business dinner tonight for 4 people.");

    RequirementsAlignmentLoop.TurnResult result = alignmentLoop.handleTurn(state, "not sure");

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.state().getEventRequirements())
        .isSameAs(initialContext.getEventRequirements());
    assertThat(result.assistantReply()).contains("next detail");
    assertThat(requirementsExtractor.extractCalls).isEqualTo(1);
  }

  @Test
  void correctiveReplyRevisesPlanAndReturnsToConfirmation() {
    JarvisAgentContext state = new JarvisAgentContext();
    requirementsExtractor.extractedContexts.add(
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 11),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Business dinner"),
            List.of()));
    alignmentLoop.handleTurn(state, "I need a business dinner tonight for 4 people.");

    requirementsExtractor.extractedContexts.add(
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 12),
                LocalTime.of(12, 0),
                4,
                MealType.LUNCH,
                "Business lunch"),
            List.of(attendee("Alex", "Union Station"))));

    RequirementsAlignmentLoop.TurnResult result =
        alignmentLoop.handleTurn(state, "Not dinner, it's lunch tomorrow.");

    assertThat(result.eventName()).isEqualTo("plan-updated");
    assertThat(result.state().getEventRequirements().getMealType()).isEqualTo(MealType.LUNCH);
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(requirementsExtractor.extractCalls).isEqualTo(2);
  }

  @Test
  void initialVagueRequestMovesDirectlyToClarification() {
    JarvisAgentContext state = new JarvisAgentContext();
    requirementsExtractor.extractedContexts.add(
        planningContext(eventRequirements(null, null, null, null, "Business meal"), List.of()));

    RequirementsAlignmentLoop.TurnResult result =
        alignmentLoop.handleTurn(state, "Help me plan something.");

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.assistantReply()).contains("date");
  }

  @Test
  void openerCreatesStarterPlanWithoutCallingModel() {
    JarvisAgentContext state = new JarvisAgentContext();

    RequirementsAlignmentLoop.TurnResult result = alignmentLoop.handleTurn(state, "hello");

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.assistantReply()).contains("date");
    assertThat(requirementsExtractor.extractCalls).isZero();
  }

  private EventRequirements eventRequirements(
      LocalDate date, LocalTime time, Integer partySize, MealType mealType, String purpose) {
    EventRequirements eventRequirements = new EventRequirements();
    eventRequirements.setDate(date);
    eventRequirements.setTime(time);
    eventRequirements.setPartySize(partySize);
    eventRequirements.setMealType(mealType);
    eventRequirements.setPurpose(purpose);
    return eventRequirements;
  }

  private Attendee attendee(String name, String origin) {
    Attendee attendee = new Attendee();
    attendee.setName(name);
    attendee.setOrigin(origin);
    attendee.setTravelMode(TravelMode.TRANSIT);
    attendee.setDietaryConstraints(List.of(DietaryConstraint.VEGETARIAN));
    return attendee;
  }

  private RequirementsExtractor.ExtractedPlanningContext planningContext(
      EventRequirements eventRequirements, List<Attendee> attendees) {
    RequirementsExtractor.ExtractedPlanningContext context =
        new RequirementsExtractor.ExtractedPlanningContext();
    context.setEventRequirements(eventRequirements);
    context.setAttendees(attendees);
    return context;
  }

  private static final class FakeRequirementsExtractor extends RequirementsExtractor {
    private final Queue<RequirementsExtractor.ExtractedPlanningContext> extractedContexts =
        new ArrayDeque<>();
    private int extractCalls;

    private FakeRequirementsExtractor() {
      super();
    }

    @Override
    public RequirementsExtractor.ExtractedPlanningContext extract(
        JarvisAgentContext existingState, String userMessage) {
      extractCalls++;
      return extractedContexts.remove();
    }
  }
}
