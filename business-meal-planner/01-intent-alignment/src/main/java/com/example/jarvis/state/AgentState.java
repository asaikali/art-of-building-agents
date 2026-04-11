package com.example.jarvis.state;

import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.EventRequirements;
import java.util.List;

public class AgentState {

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
}
