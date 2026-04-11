package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.session.SessionId;
import com.example.jarvis.agent.AgentState;
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

class IntentAlignmentConversationServiceTest {

  private FakeIntentAlignmentExtractor extractor;
  private IntentAlignmentConversationService service;

  @BeforeEach
  void setUp() {
    extractor = new FakeIntentAlignmentExtractor();
    service = new IntentAlignmentConversationService(extractor);
  }

  @Test
  void initialRequestCreatesPlanAndWaitsForConfirmation() {
    SessionId sessionId = new SessionId(1);
    extractor.extractedContexts.add(
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 11),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Client dinner"),
            List.of(attendee("Alex", "100 King St W"))));

    IntentAlignmentConversationService.TurnResult result =
        service.handleTurn(
            sessionId, "Book a client dinner tonight at 7 PM for four people from 100 King St W.");

    assertThat(result.eventName()).isEqualTo("plan-generated");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(result.assistantReply()).contains("Please confirm or correct");
    assertThat(service.getState(sessionId).map(AgentState::getEventRequirements))
        .hasValue(result.state().getEventRequirements());
  }

  @Test
  void affirmativeReplyConfirmsExistingRequirements() {
    SessionId sessionId = new SessionId(2);
    extractor.extractedContexts.add(
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 12), LocalTime.of(12, 0), 6, MealType.LUNCH, "Team lunch"),
            List.of()));
    service.handleTurn(sessionId, "I need a team lunch tomorrow for 6 people.");

    IntentAlignmentConversationService.TurnResult result = service.handleTurn(sessionId, "yes");

    assertThat(result.eventName()).isEqualTo("requirements-confirmed");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
    assertThat(result.assistantReply()).contains("confirmed");
  }

  @Test
  void nonActionableReplyRequestsClarificationWithoutChangingContext() {
    SessionId sessionId = new SessionId(3);
    IntentAlignmentExtractor.ExtractedPlanningContext initialContext =
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 11),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Business dinner"),
            List.of(attendee("Alex", "Union Station")));
    extractor.extractedContexts.add(initialContext);
    service.handleTurn(sessionId, "I need a business dinner tonight for 4 people.");

    IntentAlignmentConversationService.TurnResult result =
        service.handleTurn(sessionId, "not sure");

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.state().getEventRequirements())
        .isSameAs(initialContext.getEventRequirements());
    assertThat(result.assistantReply()).contains("next detail");
    assertThat(extractor.extractCalls).isEqualTo(1);
  }

  @Test
  void correctiveReplyRevisesPlanAndReturnsToConfirmation() {
    SessionId sessionId = new SessionId(4);
    extractor.extractedContexts.add(
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 11),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Business dinner"),
            List.of()));
    service.handleTurn(sessionId, "I need a business dinner tonight for 4 people.");

    extractor.extractedContexts.add(
        planningContext(
            eventRequirements(
                LocalDate.of(2026, 4, 12),
                LocalTime.of(12, 0),
                4,
                MealType.LUNCH,
                "Business lunch"),
            List.of(attendee("Alex", "Union Station"))));

    IntentAlignmentConversationService.TurnResult result =
        service.handleTurn(sessionId, "Not dinner, it's lunch tomorrow.");

    assertThat(result.eventName()).isEqualTo("plan-updated");
    assertThat(result.state().getEventRequirements().getMealType()).isEqualTo(MealType.LUNCH);
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(extractor.extractCalls).isEqualTo(2);
  }

  @Test
  void initialVagueRequestMovesDirectlyToClarification() {
    SessionId sessionId = new SessionId(5);
    extractor.extractedContexts.add(
        planningContext(eventRequirements(null, null, null, null, "Business meal"), List.of()));

    IntentAlignmentConversationService.TurnResult result =
        service.handleTurn(sessionId, "Help me plan something.");

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.assistantReply()).contains("date");
  }

  @Test
  void openerCreatesStarterPlanWithoutCallingModel() {
    SessionId sessionId = new SessionId(6);

    IntentAlignmentConversationService.TurnResult result = service.handleTurn(sessionId, "hello");

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.assistantReply()).contains("date");
    assertThat(extractor.extractCalls).isZero();
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

  private IntentAlignmentExtractor.ExtractedPlanningContext planningContext(
      EventRequirements eventRequirements, List<Attendee> attendees) {
    IntentAlignmentExtractor.ExtractedPlanningContext context =
        new IntentAlignmentExtractor.ExtractedPlanningContext();
    context.setEventRequirements(eventRequirements);
    context.setAttendees(attendees);
    return context;
  }

  private static final class FakeIntentAlignmentExtractor extends IntentAlignmentExtractor {
    private final Queue<IntentAlignmentExtractor.ExtractedPlanningContext> extractedContexts =
        new ArrayDeque<>();
    private int extractCalls;

    private FakeIntentAlignmentExtractor() {
      super();
    }

    @Override
    public IntentAlignmentExtractor.ExtractedPlanningContext extract(
        AgentState existingState, String userMessage) {
      extractCalls++;
      return extractedContexts.remove();
    }
  }
}
