package com.example.jarvis.requirements.alignment;

import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.EventRequirements;
import com.example.jarvis.state.AgentState;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class IntentAlignmentMarkdownRenderer {

  public String render(AgentState state) {
    EventRequirements eventRequirements = state.getEventRequirements();
    return """
        ## Event Requirements
        - Date: %s
        - Time: %s
        - Party Size: %s
        - Meal Type: %s
        - Purpose: %s
        - Budget Per Person: %s
        - Noise Level: %s

        ## Additional Requirements
        %s

        ## Cuisine Preferences
        %s

        ## Attendees
        %s

        ## Missing Information
        %s

        ## Status
        %s
        """
        .formatted(
            renderValue(eventRequirements == null ? null : eventRequirements.getDate()),
            renderValue(eventRequirements == null ? null : eventRequirements.getTime()),
            renderValue(eventRequirements == null ? null : eventRequirements.getPartySize()),
            renderEnum(eventRequirements == null ? null : eventRequirements.getMealType()),
            renderValue(eventRequirements == null ? null : eventRequirements.getPurpose()),
            renderValue(eventRequirements == null ? null : eventRequirements.getBudgetPerPerson()),
            renderEnum(eventRequirements == null ? null : eventRequirements.getNoiseLevel()),
            renderList(
                eventRequirements == null
                    ? List.of()
                    : eventRequirements.getAdditionalRequirements()),
            renderList(
                eventRequirements == null ? List.of() : eventRequirements.getCuisinePreferences()),
            renderAttendees(state.getAttendees()),
            renderList(state.getMissingInformation()),
            state.getStatus().label());
  }

  private String renderValue(Object value) {
    return value == null ? "Missing" : value.toString();
  }

  private String renderEnum(Enum<?> value) {
    if (value == null) {
      return "Missing";
    }
    return value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
  }

  private String renderList(List<String> items) {
    if (items.isEmpty()) {
      return "- None";
    }
    return items.stream()
        .map(item -> "- " + item)
        .collect(java.util.stream.Collectors.joining("\n"));
  }

  private String renderAttendees(List<Attendee> attendees) {
    if (attendees.isEmpty()) {
      return "- None";
    }
    return attendees.stream()
        .map(this::renderAttendee)
        .collect(java.util.stream.Collectors.joining("\n"));
  }

  private String renderAttendee(Attendee attendee) {
    String dietaryConstraints =
        attendee.getDietaryConstraints().isEmpty()
            ? "none"
            : attendee.getDietaryConstraints().stream()
                .map(Enum::name)
                .map(value -> value.toLowerCase(Locale.ROOT).replace('_', ' '))
                .collect(java.util.stream.Collectors.joining(", "));

    return "- Name: %s | Origin: %s | Departure Time: %s | Travel Mode: %s | Max Travel Time: %s | Max Distance: %s | Dietary Constraints: %s"
        .formatted(
            renderValue(attendee.getName()),
            renderValue(attendee.getOrigin()),
            renderValue(attendee.getDepartureTime()),
            renderEnum(attendee.getTravelMode()),
            renderValue(attendee.getMaxTravelTimeMinutes()),
            renderValue(attendee.getMaxDistanceKm()),
            dietaryConstraints);
  }
}
