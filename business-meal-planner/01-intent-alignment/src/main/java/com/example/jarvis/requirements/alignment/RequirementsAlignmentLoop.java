package com.example.jarvis.requirements.alignment;

import com.example.agent.core.session.Session;
import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.agent.RequirementStatus;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.EventRequirements;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RequirementsAlignmentLoop {

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

  private final RequirementsExtractor requirementsExtractor;

  public RequirementsAlignmentLoop(RequirementsExtractor requirementsExtractor) {
    this.requirementsExtractor = requirementsExtractor;
  }

  public TurnResult handleTurn(Session session, String userMessage) {
    JarvisAgentContext state =
        session
            .getAgentContext()
            .filter(JarvisAgentContext.class::isInstance)
            .map(JarvisAgentContext.class::cast)
            .orElseGet(
                () -> {
                  JarvisAgentContext newContext = new JarvisAgentContext();
                  session.setAgentContext(newContext);
                  return newContext;
                });
    return handleTurn(state, userMessage);
  }

  TurnResult handleTurn(JarvisAgentContext state, String userMessage) {
    boolean hasExistingContext =
        state.getEventRequirements() != null || !state.getAttendees().isEmpty();

    if (!hasExistingContext && isOpener(userMessage)) {
      initializeStateForClarification(state);
      return new TurnResult(state, buildReply(state), "clarification-requested");
    }

    if (hasExistingContext && isAffirmative(userMessage)) {
      state.setStatus(RequirementStatus.REQUIREMENTS_CONFIRMED);
      return new TurnResult(state, buildReply(state), "requirements-confirmed");
    }

    if (hasExistingContext && isUncertain(userMessage)) {
      state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
      return new TurnResult(state, buildReply(state), "clarification-requested");
    }

    RequirementsExtractor.ExtractedPlanningContext extracted =
        requirementsExtractor.extract(hasExistingContext ? state : null, userMessage);
    state.setEventRequirements(extracted.getEventRequirements());
    state.setAttendees(extracted.getAttendees());
    state.setMissingInformation(missingCriticalFields(extracted.getEventRequirements()));
    state.setStatus(decideStatus(state));

    return new TurnResult(
        state, buildReply(state), chooseAction(hasExistingContext, state.getStatus()));
  }

  private void initializeStateForClarification(JarvisAgentContext state) {
    state.setEventRequirements(new EventRequirements());
    state.setAttendees(List.of());
    state.setMissingInformation(REQUIRED_FIELDS);
    state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);
  }

  private RequirementStatus decideStatus(JarvisAgentContext state) {
    if (!state.getMissingInformation().isEmpty()) {
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

  private String buildReply(JarvisAgentContext state) {
    return switch (state.getStatus()) {
      case REQUIREMENTS_CONFIRMED -> "Great. I've captured the requirements and they're confirmed.";
      case WAITING_FOR_CLARIFICATION -> buildClarificationReply(state);
      case WAITING_FOR_CONFIRMATION -> buildConfirmationReply(state);
    };
  }

  private String buildClarificationReply(JarvisAgentContext state) {
    List<String> missing = state.getMissingInformation();
    String field = missing.isEmpty() ? "next detail" : missing.getFirst();

    return "I have the start of the plan. Before I go further, what is the "
        + field.toLowerCase(Locale.ROOT)
        + "?";
  }

  private String buildConfirmationReply(JarvisAgentContext state) {
    EventRequirements eventRequirements = state.getEventRequirements();
    List<String> summaryLines = new ArrayList<>();
    summaryLines.add("Here's my understanding so far:");
    if (eventRequirements.getDate() != null) {
      summaryLines.add("- Date: " + eventRequirements.getDate());
    }
    if (eventRequirements.getTime() != null) {
      summaryLines.add("- Time: " + eventRequirements.getTime());
    }
    if (eventRequirements.getPartySize() != null) {
      summaryLines.add("- Party Size: " + eventRequirements.getPartySize());
    }
    if (eventRequirements.getMealType() != null) {
      summaryLines.add("- Meal Type: " + humanize(eventRequirements.getMealType().name()));
    }
    if (eventRequirements.getPurpose() != null) {
      summaryLines.add("- Purpose: " + eventRequirements.getPurpose());
    }
    if (eventRequirements.getBudgetPerPerson() != null) {
      summaryLines.add("- Budget Per Person: " + eventRequirements.getBudgetPerPerson());
    }
    if (eventRequirements.getNoiseLevel() != null) {
      summaryLines.add("- Noise Level: " + humanize(eventRequirements.getNoiseLevel().name()));
    }
    for (String cuisinePreference : eventRequirements.getCuisinePreferences()) {
      summaryLines.add("- Cuisine Preference: " + cuisinePreference);
    }
    for (String requirement : eventRequirements.getAdditionalRequirements()) {
      summaryLines.add("- Requirement: " + requirement);
    }
    for (Attendee attendee : state.getAttendees()) {
      summaryLines.add("- Attendee: " + summarizeAttendee(attendee));
    }
    summaryLines.add("Please confirm or correct anything I should change.");
    return String.join("\n", summaryLines);
  }

  private String summarizeAttendee(Attendee attendee) {
    List<String> parts = new ArrayList<>();
    if (attendee.getName() != null) {
      parts.add(attendee.getName());
    }
    if (attendee.getOrigin() != null) {
      parts.add("from " + attendee.getOrigin());
    }
    if (attendee.getDepartureTime() != null) {
      parts.add("leaving at " + attendee.getDepartureTime());
    }
    if (attendee.getTravelMode() != null) {
      parts.add("via " + humanize(attendee.getTravelMode().name()));
    }
    if (!attendee.getDietaryConstraints().isEmpty()) {
      parts.add(
          "dietary: "
              + attendee.getDietaryConstraints().stream()
                  .map(Enum::name)
                  .map(this::humanize)
                  .collect(java.util.stream.Collectors.joining(", ")));
    }
    return parts.isEmpty() ? "details captured" : String.join(", ", parts);
  }

  private List<String> missingCriticalFields(EventRequirements eventRequirements) {
    List<String> missing = new ArrayList<>();
    if (eventRequirements == null || eventRequirements.getDate() == null) {
      missing.add("Date");
    }
    if (eventRequirements == null || eventRequirements.getTime() == null) {
      missing.add("Time");
    }
    if (eventRequirements == null
        || eventRequirements.getPartySize() == null
        || eventRequirements.getPartySize() <= 0) {
      missing.add("Party Size");
    }
    return missing;
  }

  private String humanize(String value) {
    return value.toLowerCase(Locale.ROOT).replace('_', ' ');
  }

  public record TurnResult(JarvisAgentContext state, String assistantReply, String eventName) {}

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
