package com.example.jarvis.constraints.deterministic.travel;

/** Possible outcomes of {@link TravelTimeCheck}. */
public enum TravelTimeCheckStatus {
  /** Estimated travel time fits within the attendee's limit. */
  PASS,
  /** Estimated travel time exceeds the attendee's limit. */
  FAIL,
  /** Required travel inputs (origin, mode, or limit) were missing. */
  UNSURE
}
