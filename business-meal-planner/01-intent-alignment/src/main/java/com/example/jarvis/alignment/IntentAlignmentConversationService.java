package com.example.jarvis.alignment;

import com.example.agent.core.session.SessionId;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class IntentAlignmentConversationService {

  private static final java.util.Set<String> AFFIRMATIVE_REPLIES =
      java.util.Set.of(
          "yes",
          "y",
          "correct",
          "looks good",
          "look good",
          "exactly",
          "that's right",
          "thats right",
          "right",
          "sounds good",
          "confirmed");

  private static final java.util.Set<String> NON_ACTIONABLE_REPLIES =
      java.util.Set.of(
          "maybe",
          "not sure",
          "unsure",
          "i don't know",
          "i dont know",
          "don't know",
          "dont know",
          "whatever",
          "you decide",
          "anything",
          "help",
          "what do you need");

  private final IntentAlignmentSessionStore sessionStore;
  private final IntentAlignmentModelClient modelClient;

  public IntentAlignmentConversationService(
      IntentAlignmentSessionStore sessionStore, IntentAlignmentModelClient modelClient) {
    this.sessionStore = sessionStore;
    this.modelClient = modelClient;
  }

  public IntentAlignmentTurnResult handleTurn(SessionId sessionId, String userMessage) {
    return sessionStore
        .findPlan(sessionId)
        .map(requirements -> handleExistingPlan(sessionId, requirements, userMessage))
        .orElseGet(() -> handleInitialRequest(sessionId, userMessage));
  }

  private IntentAlignmentTurnResult handleInitialRequest(SessionId sessionId, String userMessage) {
    RequirementStatus status = inferOpenStatus(userMessage, null, false);
    BusinessMealRequirements requirements = modelClient.createInitialPlan(userMessage, status);
    BusinessMealRequirements normalizedRequirements =
        requirements.withStatus(inferOpenStatus(userMessage, requirements, false));
    sessionStore.savePlan(sessionId, normalizedRequirements);
    return new IntentAlignmentTurnResult(
        normalizedRequirements,
        modelClient.summarizePlan(normalizedRequirements),
        normalizedRequirements.status() == RequirementStatus.WAITING_FOR_CLARIFICATION
            ? IntentAlignmentAction.CLARIFICATION_REQUESTED
            : IntentAlignmentAction.PLAN_GENERATED);
  }

  private IntentAlignmentTurnResult handleExistingPlan(
      SessionId sessionId, BusinessMealRequirements existingRequirements, String userMessage) {
    if (isAffirmative(userMessage)) {
      BusinessMealRequirements confirmedRequirements =
          existingRequirements.withStatus(RequirementStatus.REQUIREMENTS_CONFIRMED);
      sessionStore.savePlan(sessionId, confirmedRequirements);
      return new IntentAlignmentTurnResult(
          confirmedRequirements,
          modelClient.summarizePlan(confirmedRequirements),
          IntentAlignmentAction.REQUIREMENTS_CONFIRMED);
    }

    if (isNonActionable(userMessage)) {
      BusinessMealRequirements clarificationRequirements =
          existingRequirements.withStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
      sessionStore.savePlan(sessionId, clarificationRequirements);
      return new IntentAlignmentTurnResult(
          clarificationRequirements,
          modelClient.summarizePlan(clarificationRequirements),
          IntentAlignmentAction.CLARIFICATION_REQUESTED);
    }

    RequirementStatus status = inferOpenStatus(userMessage, existingRequirements, false);
    BusinessMealRequirements revisedRequirements =
        modelClient.revisePlan(existingRequirements, userMessage, status);
    BusinessMealRequirements normalizedRequirements =
        revisedRequirements.withStatus(inferOpenStatus(userMessage, revisedRequirements, false));
    sessionStore.savePlan(sessionId, normalizedRequirements);
    return new IntentAlignmentTurnResult(
        normalizedRequirements,
        modelClient.summarizePlan(normalizedRequirements),
        normalizedRequirements.status() == RequirementStatus.WAITING_FOR_CLARIFICATION
            ? IntentAlignmentAction.CLARIFICATION_REQUESTED
            : IntentAlignmentAction.PLAN_UPDATED);
  }

  private RequirementStatus inferOpenStatus(
      String userMessage, BusinessMealRequirements requirements, boolean forceClarification) {
    if (forceClarification || isNonActionable(userMessage)) {
      return RequirementStatus.WAITING_FOR_CLARIFICATION;
    }
    if (requirements == null) {
      return RequirementStatus.WAITING_FOR_CONFIRMATION;
    }
    if (requirements.explicitConstraints().isEmpty()) {
      return RequirementStatus.WAITING_FOR_CLARIFICATION;
    }
    return RequirementStatus.WAITING_FOR_CONFIRMATION;
  }

  static boolean isAffirmative(String text) {
    String normalized = normalize(text);
    return AFFIRMATIVE_REPLIES.contains(normalized);
  }

  static boolean isNonActionable(String text) {
    String normalized = normalize(text);
    return NON_ACTIONABLE_REPLIES.contains(normalized);
  }

  private static String normalize(String text) {
    if (text == null) {
      return "";
    }
    String normalized = text.toLowerCase(Locale.ROOT).trim();
    normalized = normalized.replaceAll("[.!?]+$", "");
    normalized = normalized.replaceAll("\\s+", " ");
    return normalized;
  }
}
