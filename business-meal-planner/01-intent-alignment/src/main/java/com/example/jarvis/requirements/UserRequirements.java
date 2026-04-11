package com.example.jarvis.requirements;

import java.util.List;

/**
 * Captures the full set of requirements gathered from the user for this phase.
 *
 * <p>{@code UserRequirements} is the top-level planning input for Jarvis. It brings together the
 * event-level requirements and the attendee-specific requirements so the rest of the system can
 * work with one clear representation of what the user wants.
 */
public class UserRequirements {

  private EventRequirements eventRequirements = new EventRequirements();
  private List<Attendee> attendees = List.of();

  public UserRequirements() {}

  public EventRequirements getEventRequirements() {
    return eventRequirements;
  }

  public void setEventRequirements(EventRequirements eventRequirements) {
    this.eventRequirements =
        eventRequirements == null ? new EventRequirements() : eventRequirements;
  }

  public List<Attendee> getAttendees() {
    return attendees;
  }

  public void setAttendees(List<Attendee> attendees) {
    this.attendees = attendees == null ? List.of() : List.copyOf(attendees);
  }

  public String toMarkdown() {
    return """
        %s

        ## Attendees
        %s
        """
        .formatted(eventRequirements.toMarkdown(), renderAttendees(attendees));
  }

  public boolean isEmpty() {
    return eventRequirements.getDate() == null
        && eventRequirements.getTime() == null
        && eventRequirements.getPartySize() == null
        && eventRequirements.getMealType() == null
        && eventRequirements.getPurpose() == null
        && eventRequirements.getBudgetPerPerson() == null
        && eventRequirements.getNoiseLevel() == null
        && eventRequirements.getAdditionalRequirements().isEmpty()
        && eventRequirements.getCuisinePreferences().isEmpty()
        && attendees.isEmpty();
  }

  private static String renderAttendees(List<Attendee> attendees) {
    if (attendees.isEmpty()) {
      return "- None";
    }
    return attendees.stream()
        .map(Attendee::toMarkdown)
        .collect(java.util.stream.Collectors.joining("\n"));
  }
}
