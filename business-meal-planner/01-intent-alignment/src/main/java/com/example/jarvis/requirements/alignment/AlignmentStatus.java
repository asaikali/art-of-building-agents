package com.example.jarvis.requirements.alignment;

public enum AlignmentStatus {
  WAITING_FOR_CONFIRMATION("Waiting for confirmation", "plan-updated"),
  WAITING_FOR_CLARIFICATION("Waiting for clarification", "clarification-requested"),
  REQUIREMENTS_CONFIRMED("Requirements confirmed", "requirements-confirmed");

  private final String label;
  private final String eventName;

  AlignmentStatus(String label, String eventName) {
    this.label = label;
    this.eventName = eventName;
  }

  public String label() {
    return label;
  }

  public String eventName() {
    return eventName;
  }
}
