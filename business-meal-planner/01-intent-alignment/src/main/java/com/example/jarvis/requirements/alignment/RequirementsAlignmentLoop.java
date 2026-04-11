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

  public TurnResult handleTurn(JarvisAgentContext context, String userMessage) {
    boolean hasExistingRequirements = hasExistingRequirements(context);

    if (isFirstTurnOpener(context, userMessage)) {
      return startClarification(context);
    }

    if (isConfirmationTurn(context, userMessage)) {
      return confirmRequirements(context);
    }

    if (isUncertainTurn(context, userMessage)) {
      return requestClarification(context);
    }

    UserRequirements updatedRequirements = updateRequirements(context, userMessage);
    updateWorkflowState(context, updatedRequirements);

    return new TurnResult(
        context,
        requirementsReplyBuilder.buildReply(context),
        chooseAction(hasExistingRequirements, context.getStatus()));
  }

  private boolean hasExistingRequirements(JarvisAgentContext context) {
    return !context.getUserRequirements().isEmpty();
  }

  private boolean isFirstTurnOpener(JarvisAgentContext context, String userMessage) {
    return !hasExistingRequirements(context) && turnClassifier.isOpener(userMessage);
  }

  private boolean isConfirmationTurn(JarvisAgentContext context, String userMessage) {
    return hasExistingRequirements(context) && turnClassifier.isAffirmative(userMessage);
  }

  private boolean isUncertainTurn(JarvisAgentContext context, String userMessage) {
    return hasExistingRequirements(context) && turnClassifier.isUncertain(userMessage);
  }

  private TurnResult startClarification(JarvisAgentContext context) {
    initializeStateForClarification(context);
    return new TurnResult(
        context, requirementsReplyBuilder.buildReply(context), "clarification-requested");
  }

  private TurnResult confirmRequirements(JarvisAgentContext context) {
    context.setStatus(RequirementStatus.REQUIREMENTS_CONFIRMED);
    return new TurnResult(
        context, requirementsReplyBuilder.buildReply(context), "requirements-confirmed");
  }

  private TurnResult requestClarification(JarvisAgentContext context) {
    context.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
    return new TurnResult(
        context, requirementsReplyBuilder.buildReply(context), "clarification-requested");
  }

  private UserRequirements updateRequirements(JarvisAgentContext context, String userMessage) {
    return requirementsExtractor.extract(
        hasExistingRequirements(context) ? context : null, userMessage);
  }

  private void updateWorkflowState(
      JarvisAgentContext context, UserRequirements updatedRequirements) {
    context.setUserRequirements(updatedRequirements);
    context.setMissingInformation(
        requirementsCompletionPolicy.missingCriticalFields(updatedRequirements.getMeal()));
    context.setStatus(requirementsCompletionPolicy.decideStatus(context));
  }

  private void initializeStateForClarification(JarvisAgentContext context) {
    UserRequirements userRequirements = new UserRequirements();
    userRequirements.setMeal(new Meal());
    userRequirements.setAttendees(List.of());
    context.setUserRequirements(userRequirements);
    context.setMissingInformation(REQUIRED_FIELDS);
    context.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
  }

  private String chooseAction(boolean hasExistingPlan, RequirementStatus status) {
    if (status == RequirementStatus.WAITING_FOR_CLARIFICATION) {
      return "clarification-requested";
    }
    return hasExistingPlan ? "plan-updated" : "plan-generated";
  }

  public record TurnResult(JarvisAgentContext state, String assistantReply, String eventName) {}
}
