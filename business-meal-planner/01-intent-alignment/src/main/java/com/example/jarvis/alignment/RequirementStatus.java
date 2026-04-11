package com.example.jarvis.alignment;

public enum RequirementStatus {
  WAITING_FOR_CONFIRMATION("Waiting for confirmation"),
  WAITING_FOR_CLARIFICATION("Waiting for clarification"),
  REQUIREMENTS_CONFIRMED("Requirements confirmed");

  private final String label;

  RequirementStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
