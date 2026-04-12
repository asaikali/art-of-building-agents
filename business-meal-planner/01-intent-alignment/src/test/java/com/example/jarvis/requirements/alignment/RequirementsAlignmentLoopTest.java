package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.chat.AgentMessage;
import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.MealType;
import com.example.jarvis.requirements.TravelMode;
import com.example.jarvis.requirements.UserRequirements;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequirementsAlignmentLoopTest {

  private FakeRequirementsExtractor requirementsExtractor;
  private FakeRequirementsReplyWriter replyWriter;
  private RequirementsAlignmentLoop alignmentLoop;

  @BeforeEach
  void setUp() {
    requirementsExtractor = new FakeRequirementsExtractor();
    replyWriter = new FakeRequirementsReplyWriter();
    alignmentLoop =
        new RequirementsAlignmentLoop(
            requirementsExtractor, new RequirementsCompletionPolicy(), replyWriter);
  }

  @Test
  void richFirstTurnWaitsForConfirmation() {
    JarvisAgentContext context = new JarvisAgentContext();
    requirementsExtractor.enqueue(
        requirements(
            meal(
                LocalDate.of(2026, 4, 13),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Client dinner"),
            List.of(attendee("Alex", "100 King St W"))));

    RequirementsAlignmentLoop.TurnResult result =
        alignmentLoop.handleTurn(context, "Client dinner tomorrow at 7 for 4 people.", List.of());

    assertThat(result.eventName()).isEqualTo("plan-updated");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(result.state().getMissingInformation()).isEmpty();
    assertThat(replyWriter.lastDirective().status())
        .isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
  }

  @Test
  void vagueFirstTurnWaitsForClarification() {
    JarvisAgentContext context = new JarvisAgentContext();
    requirementsExtractor.enqueue(
        requirements(meal(null, null, null, null, "Business meal"), List.of()));

    RequirementsAlignmentLoop.TurnResult result =
        alignmentLoop.handleTurn(context, "Help me plan something.", List.of());

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(result.state().getMissingInformation()).contains("Date");
    assertThat(replyWriter.lastDirective().missingCriticalFields()).contains("Date");
  }

  @Test
  void greetingTurnExtractsAndClarifies() {
    JarvisAgentContext context = new JarvisAgentContext();
    // Extractor sees "hello" against empty state and returns empty state unchanged
    requirementsExtractor.enqueue(new UserRequirements());

    RequirementsAlignmentLoop.TurnResult result =
        alignmentLoop.handleTurn(context, "hello", List.of());

    assertThat(result.eventName()).isEqualTo("clarification-requested");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);
    assertThat(requirementsExtractor.extractCalls).isEqualTo(1);
  }

  @Test
  void correctionTurnUpdatesRequirements() {
    JarvisAgentContext context = new JarvisAgentContext();
    // First turn: dinner
    requirementsExtractor.enqueue(
        requirements(
            meal(
                LocalDate.of(2026, 4, 13),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Business dinner"),
            List.of()));
    alignmentLoop.handleTurn(context, "Business dinner tomorrow at 7 for 4 people.", List.of());

    // Second turn: correction to lunch
    requirementsExtractor.enqueue(
        requirements(
            meal(
                LocalDate.of(2026, 4, 14),
                LocalTime.of(12, 0),
                4,
                MealType.LUNCH,
                "Business lunch"),
            List.of(attendee("Alex", "Union Station"))));

    RequirementsAlignmentLoop.TurnResult result =
        alignmentLoop.handleTurn(context, "Not dinner, it's lunch tomorrow.", List.of());

    assertThat(result.eventName()).isEqualTo("plan-updated");
    assertThat(result.state().getUserRequirements().getMeal().getMealType())
        .isEqualTo(MealType.LUNCH);
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(requirementsExtractor.extractCalls).isEqualTo(2);
  }

  @Test
  void confirmationTurnDetectedByEqualRequirements() {
    JarvisAgentContext context = new JarvisAgentContext();
    UserRequirements completeRequirements =
        requirements(
            meal(
                LocalDate.of(2026, 4, 13),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Client dinner"),
            List.of());

    // First turn: set up complete requirements and WAITING_FOR_CONFIRMATION status
    requirementsExtractor.enqueue(completeRequirements);
    alignmentLoop.handleTurn(context, "Client dinner tomorrow at 7 for 4.", List.of());
    assertThat(context.getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);

    // Second turn: user says "yes" — extractor returns identical requirements
    requirementsExtractor.enqueue(
        requirements(
            meal(
                LocalDate.of(2026, 4, 13),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Client dinner"),
            List.of()));

    RequirementsAlignmentLoop.TurnResult result =
        alignmentLoop.handleTurn(context, "yes", List.of());

    assertThat(result.eventName()).isEqualTo("requirements-confirmed");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
  }

  @Test
  void confirmationWithCorrectionDoesNotConfirm() {
    JarvisAgentContext context = new JarvisAgentContext();
    // First turn: complete requirements
    requirementsExtractor.enqueue(
        requirements(
            meal(
                LocalDate.of(2026, 4, 13),
                LocalTime.of(19, 0),
                4,
                MealType.DINNER,
                "Client dinner"),
            List.of()));
    alignmentLoop.handleTurn(context, "Client dinner tomorrow at 7 for 4.", List.of());
    assertThat(context.getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);

    // Second turn: "yes but change to lunch" — extractor returns different requirements
    requirementsExtractor.enqueue(
        requirements(
            meal(LocalDate.of(2026, 4, 13), LocalTime.of(12, 0), 4, MealType.LUNCH, "Client lunch"),
            List.of()));

    RequirementsAlignmentLoop.TurnResult result =
        alignmentLoop.handleTurn(context, "yes but make it lunch", List.of());

    assertThat(result.eventName()).isEqualTo("plan-updated");
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
  }

  private Meal meal(
      LocalDate date, LocalTime time, Integer partySize, MealType mealType, String purpose) {
    Meal meal = new Meal();
    meal.setDate(date);
    meal.setTime(time);
    meal.setPartySize(partySize);
    meal.setMealType(mealType);
    meal.setPurpose(purpose);
    return meal;
  }

  private Attendee attendee(String name, String origin) {
    Attendee attendee = new Attendee();
    attendee.setName(name);
    attendee.setOrigin(origin);
    attendee.setTravelMode(TravelMode.TRANSIT);
    attendee.setDietaryConstraints(List.of(DietaryConstraint.VEGETARIAN));
    return attendee;
  }

  private UserRequirements requirements(Meal meal, List<Attendee> attendees) {
    UserRequirements userRequirements = new UserRequirements();
    userRequirements.setMeal(meal);
    userRequirements.setAttendees(attendees);
    return userRequirements;
  }

  private static final class FakeRequirementsExtractor extends RequirementsExtractor {

    private final Queue<UserRequirements> queue = new ArrayDeque<>();
    private int extractCalls;

    private FakeRequirementsExtractor() {
      super();
    }

    void enqueue(UserRequirements requirements) {
      queue.add(requirements);
    }

    @Override
    public UserRequirements extract(UserRequirements currentRequirements, String userMessage) {
      extractCalls++;
      return queue.remove();
    }
  }

  private static final class FakeRequirementsReplyWriter extends RequirementsReplyWriter {

    private ReplyDirective lastDirective;

    private FakeRequirementsReplyWriter() {
      super();
    }

    @Override
    public String writeReply(ReplyDirective directive, List<AgentMessage> conversationHistory) {
      this.lastDirective = directive;
      return "fake reply";
    }

    ReplyDirective lastDirective() {
      return lastDirective;
    }
  }
}
