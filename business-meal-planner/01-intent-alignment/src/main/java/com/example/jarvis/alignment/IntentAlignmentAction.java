package com.example.jarvis.alignment;

public enum IntentAlignmentAction {
  PLAN_GENERATED("plan-generated"),
  PLAN_UPDATED("plan-updated"),
  CLARIFICATION_REQUESTED("clarification-requested"),
  REQUIREMENTS_CONFIRMED("requirements-confirmed");

  private final String eventName;

  IntentAlignmentAction(String eventName) {
    this.eventName = eventName;
  }

  public String eventName() {
    return eventName;
  }
}
