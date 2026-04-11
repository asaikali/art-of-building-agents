package com.example.jarvis.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.session.SessionId;
import java.util.ArrayDeque;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntentAlignmentConversationServiceTest {

  private IntentAlignmentSessionStore sessionStore;
  private FakeIntentAlignmentModelClient modelClient;
  private IntentAlignmentConversationService service;

  @BeforeEach
  void setUp() {
    sessionStore = new IntentAlignmentSessionStore();
    modelClient = new FakeIntentAlignmentModelClient();
    service = new IntentAlignmentConversationService(sessionStore, modelClient);
  }

  @Test
  void initialRequestCreatesPlanAndWaitsForConfirmation() {
    SessionId sessionId = new SessionId(1);
    modelClient.initialPlans.add(requirements("Plan and book a business dinner.", 2));
    modelClient.summaries.add("Here is my understanding. Please confirm or correct it.");

    IntentAlignmentTurnResult result =
        service.handleTurn(sessionId, "Book a client dinner tonight for four people.");

    assertThat(result.action()).isEqualTo(IntentAlignmentAction.PLAN_GENERATED);
    assertThat(result.requirements().status())
        .isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(result.assistantReply()).contains("confirm");
    assertThat(sessionStore.findPlan(sessionId)).hasValue(result.requirements());
  }

  @Test
  void affirmativeReplyConfirmsExistingRequirements() {
    SessionId sessionId = new SessionId(2);
    modelClient.initialPlans.add(requirements("Plan a team lunch.", 1));
    modelClient.summaries.add("Please confirm or correct this summary.");
    service.handleTurn(sessionId, "I need a team lunch tomorrow.");

    modelClient.summaries.add("Requirements confirmed. Phase 1 is complete.");
    IntentAlignmentTurnResult result = service.handleTurn(sessionId, "yes");

    assertThat(result.action()).isEqualTo(IntentAlignmentAction.REQUIREMENTS_CONFIRMED);
    assertThat(result.requirements().status()).isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
    assertThat(result.assistantReply()).contains("confirmed");
  }

  @Test
  void nonActionableReplyRequestsClarificationWithoutChangingPlan() {
    SessionId sessionId = new SessionId(3);
    BusinessMealRequirements initialRequirements = requirements("Plan a business dinner.", 2);
    modelClient.initialPlans.add(initialRequirements);
    modelClient.summaries.add("Please confirm or correct this summary.");
    service.handleTurn(sessionId, "I need a business dinner tonight.");

    modelClient.summaries.add("What is the per-person budget for this meal?");
    IntentAlignmentTurnResult result = service.handleTurn(sessionId, "not sure");

    assertThat(result.action()).isEqualTo(IntentAlignmentAction.CLARIFICATION_REQUESTED);
    assertThat(result.requirements().status())
        .isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.requirements().explicitConstraints())
        .isEqualTo(initialRequirements.explicitConstraints());
    assertThat(modelClient.reviseCalls).isZero();
  }

  @Test
  void correctiveReplyRevisesPlanAndReturnsToConfirmation() {
    SessionId sessionId = new SessionId(4);
    modelClient.initialPlans.add(requirements("Plan a business dinner.", 2));
    modelClient.summaries.add("Please confirm or correct this summary.");
    service.handleTurn(sessionId, "I need a business dinner tonight.");

    modelClient.revisedPlans.add(
        new BusinessMealRequirements(
            "Plan a business lunch.",
            java.util.List.of("Lunch tomorrow", "Budget is 80 per person"),
            java.util.List.of("Venue should support business conversation"),
            java.util.List.of("Origin location"),
            java.util.List.of("Do not book until options are reviewed"),
            RequirementStatus.WAITING_FOR_CONFIRMATION));
    modelClient.summaries.add("Updated. Please confirm or correct this.");

    IntentAlignmentTurnResult result =
        service.handleTurn(
            sessionId, "Not dinner, it's lunch tomorrow and budget is 80 per person.");

    assertThat(result.action()).isEqualTo(IntentAlignmentAction.PLAN_UPDATED);
    assertThat(result.requirements().intent()).contains("lunch");
    assertThat(result.requirements().status())
        .isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(modelClient.reviseCalls).isEqualTo(1);
  }

  @Test
  void initialVagueRequestMovesDirectlyToClarification() {
    SessionId sessionId = new SessionId(5);
    modelClient.initialPlans.add(
        new BusinessMealRequirements(
            "Clarify the business meal request.",
            java.util.List.of(),
            java.util.List.of("Business appropriateness matters"),
            java.util.List.of("Meal type", "Party size", "Date and time"),
            java.util.List.of(),
            RequirementStatus.WAITING_FOR_CONFIRMATION));
    modelClient.summaries.add("What kind of business meal are you planning?");

    IntentAlignmentTurnResult result = service.handleTurn(sessionId, "Help me plan something.");

    assertThat(result.action()).isEqualTo(IntentAlignmentAction.CLARIFICATION_REQUESTED);
    assertThat(result.requirements().status())
        .isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
  }

  private BusinessMealRequirements requirements(String intent, int explicitCount) {
    java.util.List<String> explicitConstraints = new java.util.ArrayList<>();
    for (int i = 1; i <= explicitCount; i++) {
      explicitConstraints.add("Explicit constraint " + i);
    }
    return new BusinessMealRequirements(
        intent,
        explicitConstraints,
        java.util.List.of("Venue should support business conversation"),
        java.util.List.of("Budget"),
        java.util.List.of("Business appropriateness matters"),
        RequirementStatus.WAITING_FOR_CONFIRMATION);
  }

  private static final class FakeIntentAlignmentModelClient implements IntentAlignmentModelClient {
    private final Queue<BusinessMealRequirements> initialPlans = new ArrayDeque<>();
    private final Queue<BusinessMealRequirements> revisedPlans = new ArrayDeque<>();
    private final Queue<String> summaries = new ArrayDeque<>();
    private int reviseCalls;

    @Override
    public BusinessMealRequirements createInitialPlan(
        String userMessage, RequirementStatus status) {
      return initialPlans.remove().withStatus(status);
    }

    @Override
    public BusinessMealRequirements revisePlan(
        BusinessMealRequirements existingRequirements,
        String userMessage,
        RequirementStatus status) {
      reviseCalls++;
      return revisedPlans.remove().withStatus(status);
    }

    @Override
    public String summarizePlan(BusinessMealRequirements requirements) {
      return summaries.remove();
    }
  }
}
