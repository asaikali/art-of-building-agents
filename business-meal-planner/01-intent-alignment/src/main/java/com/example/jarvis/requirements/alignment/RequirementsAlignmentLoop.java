package com.example.jarvis.requirements.alignment;

import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.agent.RequirementStatus;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.UserRequirements;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RequirementsAlignmentLoop {

  private static final List<String> REQUIRED_FIELDS = List.of("Date", "Time", "Party Size");

  private final TurnClassifier turnClassifier;
  private final RequirementsExtractor requirementsExtractor;
  private final RequirementsCompletionPolicy requirementsCompletionPolicy;
  private final RequirementsReplyBuilder requirementsReplyBuilder;

  public RequirementsAlignmentLoop(
      TurnClassifier turnClassifier,
      RequirementsExtractor requirementsExtractor,
      RequirementsCompletionPolicy requirementsCompletionPolicy,
      RequirementsReplyBuilder requirementsReplyBuilder) {
    this.turnClassifier = turnClassifier;
    this.requirementsExtractor = requirementsExtractor;
    this.requirementsCompletionPolicy = requirementsCompletionPolicy;
    this.requirementsReplyBuilder = requirementsReplyBuilder;
  }

  public TurnResult handleTurn(JarvisAgentContext state, String userMessage) {
    UserRequirements userRequirements = state.getUserRequirements();
    boolean hasExistingContext = !userRequirements.isEmpty();

    if (!hasExistingContext && turnClassifier.isOpener(userMessage)) {
      initializeStateForClarification(state);
      return new TurnResult(
          state, requirementsReplyBuilder.buildReply(state), "clarification-requested");
    }

    if (hasExistingContext && turnClassifier.isAffirmative(userMessage)) {
      state.setStatus(RequirementStatus.REQUIREMENTS_CONFIRMED);
      return new TurnResult(
          state, requirementsReplyBuilder.buildReply(state), "requirements-confirmed");
    }

    if (hasExistingContext && turnClassifier.isUncertain(userMessage)) {
      state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
      return new TurnResult(
          state, requirementsReplyBuilder.buildReply(state), "clarification-requested");
    }

    UserRequirements extracted =
        requirementsExtractor.extract(hasExistingContext ? state : null, userMessage);
    state.setUserRequirements(extracted);
    state.setMissingInformation(
        requirementsCompletionPolicy.missingCriticalFields(extracted.getMeal()));
    state.setStatus(requirementsCompletionPolicy.decideStatus(state));

    return new TurnResult(
        state,
        requirementsReplyBuilder.buildReply(state),
        chooseAction(hasExistingContext, state.getStatus()));
  }

  private void initializeStateForClarification(JarvisAgentContext state) {
    UserRequirements userRequirements = new UserRequirements();
    userRequirements.setMeal(new Meal());
    userRequirements.setAttendees(List.of());
    state.setUserRequirements(userRequirements);
    state.setMissingInformation(REQUIRED_FIELDS);
    state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
  }

  private String chooseAction(boolean hasExistingPlan, RequirementStatus status) {
    if (status == RequirementStatus.WAITING_FOR_CLARIFICATION) {
      return "clarification-requested";
    }
    return hasExistingPlan ? "plan-updated" : "plan-generated";
  }

  public record TurnResult(JarvisAgentContext state, String assistantReply, String eventName) {}
}
