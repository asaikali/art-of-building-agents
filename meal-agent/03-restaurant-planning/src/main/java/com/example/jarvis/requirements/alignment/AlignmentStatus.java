package com.example.jarvis.requirements.alignment;

/**
 * The workflow status of the alignment phase. Tracks where we are in the conversation with the user
 * as we gather and confirm their meal planning requirements.
 *
 * <p>The status progresses through these states:
 *
 * <ul>
 *   <li>{@link #GATHERING_REQUIREMENTS} — required fields are missing, the agent asks the user for
 *       more information
 *   <li>{@link #CONFIRMING_REQUIREMENTS} — all required fields are present, the agent asks the user
 *       to confirm or correct before proceeding
 *   <li>{@link #REQUIREMENTS_CONFIRMED} — the user confirmed, alignment is complete and the next
 *       phase can begin
 * </ul>
 */
public enum AlignmentStatus {
  GATHERING_REQUIREMENTS("Gathering requirements"),
  CONFIRMING_REQUIREMENTS("Confirming requirements"),
  REQUIREMENTS_CONFIRMED("Requirements confirmed");

  private final String label;

  AlignmentStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
