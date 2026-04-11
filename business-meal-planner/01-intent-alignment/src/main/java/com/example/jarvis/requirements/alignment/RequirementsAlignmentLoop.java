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
    boolean hasExistingRequirements = hasExistingRequirements(state);

    if (isFirstTurnOpener(state, userMessage)) {
      return startClarification(state);
    }

    if (isConfirmationTurn(state, userMessage)) {
      return confirmRequirements(state);
    }

    if (isUncertainTurn(state, userMessage)) {
      return requestClarification(state);
    }

    UserRequirements updatedRequirements = updateRequirements(state, userMessage);
    updateWorkflowState(state, updatedRequirements);

    return new TurnResult(
        state,
        requirementsReplyBuilder.buildReply(state),
        chooseAction(hasExistingRequirements, state.getStatus()));
  }

  private boolean hasExistingRequirements(JarvisAgentContext state) {
    return !state.getUserRequirements().isEmpty();
  }

  private boolean isFirstTurnOpener(JarvisAgentContext state, String userMessage) {
    return !hasExistingRequirements(state) && turnClassifier.isOpener(userMessage);
  }

  private boolean isConfirmationTurn(JarvisAgentContext state, String userMessage) {
    return hasExistingRequirements(state) && turnClassifier.isAffirmative(userMessage);
  }

  private boolean isUncertainTurn(JarvisAgentContext state, String userMessage) {
    return hasExistingRequirements(state) && turnClassifier.isUncertain(userMessage);
  }

  private TurnResult startClarification(JarvisAgentContext state) {
    initializeStateForClarification(state);
    return new TurnResult(
        state, requirementsReplyBuilder.buildReply(state), "clarification-requested");
  }

  private TurnResult confirmRequirements(JarvisAgentContext state) {
    state.setStatus(RequirementStatus.REQUIREMENTS_CONFIRMED);
    return new TurnResult(
        state, requirementsReplyBuilder.buildReply(state), "requirements-confirmed");
  }

  private TurnResult requestClarification(JarvisAgentContext state) {
    state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
    return new TurnResult(
        state, requirementsReplyBuilder.buildReply(state), "clarification-requested");
  }

  private UserRequirements updateRequirements(JarvisAgentContext state, String userMessage) {
    return requirementsExtractor.extract(
        hasExistingRequirements(state) ? state : null, userMessage);
  }

  private void updateWorkflowState(JarvisAgentContext state, UserRequirements updatedRequirements) {
    state.setUserRequirements(updatedRequirements);
    state.setMissingInformation(
        requirementsCompletionPolicy.missingCriticalFields(updatedRequirements.getMeal()));
    state.setStatus(requirementsCompletionPolicy.decideStatus(state));
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
