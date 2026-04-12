package com.example.jarvis.requirements.alignment;

/**
 * The workflow status of the alignment phase. Tracks where we are in the conversation with the user
 * as we gather and confirm their meal planning requirements.
 *
 * <p>The status progresses through these states:
 *
 * <ul>
 *   <li>{@link #WAITING_FOR_CLARIFICATION} — required fields are missing, we need to ask the user
 *   <li>{@link #WAITING_FOR_CONFIRMATION} — all required fields are present, we need the user to
 *       confirm before proceeding
 *   <li>{@link #REQUIREMENTS_CONFIRMED} — the user confirmed, alignment is complete and the next
 *       phase can begin
 * </ul>
 */
public enum AlignmentStatus {
  WAITING_FOR_CLARIFICATION("Waiting for clarification"),
  WAITING_FOR_CONFIRMATION("Waiting for confirmation"),
  REQUIREMENTS_CONFIRMED("Requirements confirmed");

  private final String label;

  AlignmentStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
