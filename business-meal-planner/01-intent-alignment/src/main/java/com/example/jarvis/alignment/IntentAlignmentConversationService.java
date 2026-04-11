package com.example.jarvis.alignment;

import com.example.agent.core.session.SessionId;
import com.example.jarvis.state.AgentState;
import com.example.jarvis.state.UserGoals;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

  private static final java.util.Set<String> OPENERS =
      java.util.Set.of("hi", "hello", "hey", "can you help", "help", "what do you need");

  private static final java.util.Set<String> UNCERTAIN_REPLIES =
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
          "anything");

  private static final List<String> REQUIRED_FIELDS = List.of("Date", "Time", "Party Size");

  private final IntentAlignmentExtractor extractor;
  private final ConcurrentHashMap<SessionId, AgentState> statesBySession =
      new ConcurrentHashMap<>();

  public IntentAlignmentConversationService(IntentAlignmentExtractor extractor) {
    this.extractor = extractor;
  }

  public TurnResult handleTurn(SessionId sessionId, String userMessage) {
    AgentState state = statesBySession.computeIfAbsent(sessionId, id -> new AgentState());
    UserGoals existingGoals = state.userGoals().orElse(null);

    if (existingGoals == null && isOpener(userMessage)) {
      initializeStateForClarification(state, createStarterUserGoals());
      return new TurnResult(state, buildReply(state), "clarification-requested");
    }

    if (existingGoals != null && isAffirmative(userMessage)) {
      state.setStatus(RequirementStatus.REQUIREMENTS_CONFIRMED);
      return new TurnResult(state, buildReply(state), "requirements-confirmed");
    }

    if (existingGoals != null && isUncertain(userMessage)) {
      state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
      return new TurnResult(state, buildReply(state), "clarification-requested");
    }

    UserGoals extractedGoals = extractor.extractRequirements(existingGoals, userMessage);
    state.setUserGoals(extractedGoals);
    state.setMissingInformation(missingCriticalFields(extractedGoals));
    state.setAssumptions(inferAssumptions(extractedGoals));
    state.setStatus(decideStatus(state));

    return new TurnResult(
        state, buildReply(state), chooseAction(existingGoals != null, state.status()));
  }

  Optional<AgentState> getState(SessionId sessionId) {
    return Optional.ofNullable(statesBySession.get(sessionId));
  }

  private UserGoals createStarterUserGoals() {
    return new UserGoals("Clarify the business meal request.", null, null, null, List.of());
  }

  private void initializeStateForClarification(AgentState state, UserGoals userGoals) {
    state.setUserGoals(userGoals);
    state.setMissingInformation(REQUIRED_FIELDS);
    state.setAssumptions(List.of("The dining experience should fit the occasion."));
    state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
  }

  private RequirementStatus decideStatus(AgentState state) {
    if (!state.missingInformation().isEmpty()) {
      return RequirementStatus.WAITING_FOR_CLARIFICATION;
    }
    return RequirementStatus.WAITING_FOR_CONFIRMATION;
  }

  private String chooseAction(boolean hasExistingPlan, RequirementStatus status) {
    if (status == RequirementStatus.WAITING_FOR_CLARIFICATION) {
      return "clarification-requested";
    }
    return hasExistingPlan ? "plan-updated" : "plan-generated";
  }

  private String buildReply(AgentState state) {
    return switch (state.status()) {
      case REQUIREMENTS_CONFIRMED -> "Great. I've captured the requirements and they're confirmed.";
      case WAITING_FOR_CLARIFICATION -> buildClarificationReply(state);
      case WAITING_FOR_CONFIRMATION -> buildConfirmationReply(state);
    };
  }

  private String buildClarificationReply(AgentState state) {
    List<String> missing = state.missingInformation();
    String field = missing.isEmpty() ? "next detail" : missing.getFirst();

    return "I have the start of the plan. Before I go further, what is the "
        + field.toLowerCase(Locale.ROOT)
        + "?";
  }

  private String buildConfirmationReply(AgentState state) {
    UserGoals userGoals = state.userGoals().orElseThrow();
    List<String> summaryLines = new ArrayList<>();
    summaryLines.add("Here's my understanding so far:");
    if (userGoals.getDate() != null) {
      summaryLines.add("- Date: " + userGoals.getDate());
    }
    if (userGoals.getTime() != null) {
      summaryLines.add("- Time: " + userGoals.getTime());
    }
    if (userGoals.getPartySize() != null) {
      summaryLines.add("- Party Size: " + userGoals.getPartySize());
    }
    for (String constraint : userGoals.getConstraints()) {
      summaryLines.add("- " + constraint);
    }
    if (!state.assumptions().isEmpty()) {
      summaryLines.add("- Assumption: " + state.assumptions().getFirst());
    }
    summaryLines.add("Please confirm or correct anything I should change.");
    return String.join("\n", summaryLines);
  }

  private List<String> missingCriticalFields(UserGoals userGoals) {
    List<String> missing = new ArrayList<>();
    if (userGoals.getDate() == null) {
      missing.add("Date");
    }
    if (userGoals.getTime() == null) {
      missing.add("Time");
    }
    if (userGoals.getPartySize() == null || userGoals.getPartySize() <= 0) {
      missing.add("Party Size");
    }
    return missing;
  }

  private List<String> inferAssumptions(UserGoals userGoals) {
    if (userGoals.getConstraints().stream()
        .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains("client"))) {
      return List.of("The venue should support a polished business conversation.");
    }
    if (userGoals.getIntent().toLowerCase(Locale.ROOT).contains("business")) {
      return List.of("The dining experience should fit the occasion.");
    }
    return List.of();
  }

  public record TurnResult(AgentState state, String assistantReply, String eventName) {}

  static boolean isAffirmative(String text) {
    String normalized = normalize(text);
    return AFFIRMATIVE_REPLIES.contains(normalized);
  }

  static boolean isOpener(String text) {
    String normalized = normalize(text);
    return OPENERS.contains(normalized);
  }

  static boolean isUncertain(String text) {
    String normalized = normalize(text);
    return UNCERTAIN_REPLIES.contains(normalized);
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
