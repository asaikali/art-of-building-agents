package com.example.jarvis.requirements.alignment;

import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.EventRequirements;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class RequirementsReplyBuilder {

  public String buildReply(JarvisAgentContext context) {
    return switch (context.getStatus()) {
      case REQUIREMENTS_CONFIRMED -> "Great. I've captured the requirements and they're confirmed.";
      case WAITING_FOR_CLARIFICATION -> buildClarificationReply(context);
      case WAITING_FOR_CONFIRMATION -> buildConfirmationReply(context);
    };
  }

  private String buildClarificationReply(JarvisAgentContext context) {
    List<String> missing = context.getMissingInformation();
    String field = missing.isEmpty() ? "next detail" : missing.getFirst();

    return "I have the start of the plan. Before I go further, what is the "
        + field.toLowerCase(Locale.ROOT)
        + "?";
  }

  private String buildConfirmationReply(JarvisAgentContext context) {
    EventRequirements eventRequirements = context.getEventRequirements();
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
    for (Attendee attendee : context.getAttendees()) {
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

  private String humanize(String value) {
    return value.toLowerCase(Locale.ROOT).replace('_', ' ');
  }
}
