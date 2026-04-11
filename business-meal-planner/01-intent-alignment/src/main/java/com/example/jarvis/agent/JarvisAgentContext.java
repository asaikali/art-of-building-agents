package com.example.jarvis.agent;

import com.example.agent.core.session.AgentContext;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.EventRequirements;
import java.util.List;

public class JarvisAgentContext implements AgentContext {

  private EventRequirements eventRequirements;
  private List<Attendee> attendees = List.of();
  private List<String> missingInformation = List.of();
  private RequirementStatus status = RequirementStatus.WAITING_FOR_CLARIFICATION;

  public EventRequirements getEventRequirements() {
    return eventRequirements;
  }

  public void setEventRequirements(EventRequirements eventRequirements) {
    this.eventRequirements = eventRequirements;
  }

  public List<Attendee> getAttendees() {
    return attendees;
  }

  public void setAttendees(List<Attendee> attendees) {
    this.attendees = attendees == null ? List.of() : List.copyOf(attendees);
  }

  public List<String> getMissingInformation() {
    return missingInformation;
  }

  public void setMissingInformation(List<String> missingInformation) {
    this.missingInformation =
        missingInformation == null ? List.of() : List.copyOf(missingInformation);
  }

  public RequirementStatus getStatus() {
    return status;
  }

  public void setStatus(RequirementStatus status) {
    this.status = status;
  }

  public String toMarkdown() {
    EventRequirements requirements =
        eventRequirements == null ? new EventRequirements() : eventRequirements;
    return """
        %s

        ## Attendees
        %s

        ## Missing Information
        %s

        ## Status
        %s
        """
        .formatted(
            requirements.toMarkdown(),
            renderAttendees(attendees),
            renderList(missingInformation),
            status.label());
  }

  private static String renderAttendees(List<Attendee> attendees) {
    if (attendees.isEmpty()) {
      return "- None";
    }
    return attendees.stream()
        .map(Attendee::toMarkdown)
        .collect(java.util.stream.Collectors.joining("\n"));
  }

  private static String renderList(List<String> items) {
    if (items.isEmpty()) {
      return "- None";
    }
    return items.stream()
        .map(item -> "- " + item)
        .collect(java.util.stream.Collectors.joining("\n"));
  }
}
