package com.example.jarvis.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.session.SessionId;
import com.example.jarvis.state.AgentState;
import com.example.jarvis.state.UserGoals;
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
    extractor.extractedGoals.add(
        goals(
            "Plan and book a business dinner.",
            LocalDate.of(2026, 4, 11),
            LocalTime.of(19, 0),
            4,
            List.of("Client dinner", "From 100 King St W")));

    IntentAlignmentConversationService.TurnResult result =
        service.handleTurn(
            sessionId, "Book a client dinner tonight at 7 PM for four people from 100 King St W.");

    assertThat(result.eventName()).isEqualTo("plan-generated");
    assertThat(result.state().status()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(result.assistantReply()).contains("Please confirm or correct");
    assertThat(service.getState(sessionId).flatMap(AgentState::userGoals))
        .hasValue(result.state().userGoals().orElseThrow());
  }

  @Test
  void affirmativeReplyConfirmsExistingRequirements() {
    SessionId sessionId = new SessionId(2);
    extractor.extractedGoals.add(
        goals(
            "Plan a team lunch.",
            LocalDate.of(2026, 4, 12),
            LocalTime.of(12, 0),
            6,
            List.of("Team lunch", "From 200 Bay St")));
    service.handleTurn(sessionId, "I need a team lunch tomorrow from 200 Bay St for 6 people.");

    IntentAlignmentConversationService.TurnResult result = service.handleTurn(sessionId, "yes");

    assertThat(result.eventName()).isEqualTo("requirements-confirmed");
    assertThat(result.state().status()).isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
    assertThat(result.assistantReply()).contains("confirmed");
  }

  @Test
  void nonActionableReplyRequestsClarificationWithoutChangingGoals() {
    SessionId sessionId = new SessionId(3);
    UserGoals initialGoals =
        goals(
            "Plan a business dinner.",
            LocalDate.of(2026, 4, 11),
            LocalTime.of(19, 0),
            4,
            List.of("Business dinner"));
    extractor.extractedGoals.add(initialGoals);
    service.handleTurn(sessionId, "I need a business dinner tonight for 4 people.");

    IntentAlignmentConversationService.TurnResult result =
        service.handleTurn(sessionId, "not sure");

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().status()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.state().userGoals()).hasValue(initialGoals);
    assertThat(result.assistantReply()).contains("next detail");
    assertThat(extractor.extractCalls).isEqualTo(1);
  }

  @Test
  void correctiveReplyRevisesGoalsAndReturnsToConfirmation() {
    SessionId sessionId = new SessionId(4);
    extractor.extractedGoals.add(
        goals(
            "Plan a business dinner.",
            LocalDate.of(2026, 4, 11),
            LocalTime.of(19, 0),
            4,
            List.of("Business dinner")));
    service.handleTurn(sessionId, "I need a business dinner tonight for 4 people.");

    extractor.extractedGoals.add(
        goals(
            "Plan a business lunch.",
            LocalDate.of(2026, 4, 12),
            LocalTime.of(12, 0),
            4,
            List.of("Business lunch", "Budget: 80 per person")));

    IntentAlignmentConversationService.TurnResult result =
        service.handleTurn(
            sessionId, "Not dinner, it's lunch tomorrow and budget is 80 per person.");

    assertThat(result.eventName()).isEqualTo("plan-updated");
    assertThat(result.state().userGoals().orElseThrow().getIntent()).contains("lunch");
    assertThat(result.state().status()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(extractor.extractCalls).isEqualTo(2);
  }

  @Test
  void initialVagueRequestMovesDirectlyToClarification() {
    SessionId sessionId = new SessionId(5);
    extractor.extractedGoals.add(
        goals("Clarify the business meal request.", null, null, null, List.of()));

    IntentAlignmentConversationService.TurnResult result =
        service.handleTurn(sessionId, "Help me plan something.");

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().status()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.assistantReply()).contains("date");
  }

  @Test
  void openerCreatesStarterPlanWithoutCallingModel() {
    SessionId sessionId = new SessionId(6);

    IntentAlignmentConversationService.TurnResult result = service.handleTurn(sessionId, "hello");

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().status()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.assistantReply()).contains("date");
    assertThat(extractor.extractCalls).isZero();
  }

  private UserGoals goals(
      String intent, LocalDate date, LocalTime time, Integer partySize, List<String> constraints) {
    return new UserGoals(intent, date, time, partySize, constraints);
  }

  private static final class FakeIntentAlignmentExtractor extends IntentAlignmentExtractor {
    private final Queue<UserGoals> extractedGoals = new ArrayDeque<>();
    private int extractCalls;

    private FakeIntentAlignmentExtractor() {
      super();
    }

    @Override
    public UserGoals extractRequirements(UserGoals existingGoals, String userMessage) {
      extractCalls++;
      return extractedGoals.remove();
    }
  }
}
