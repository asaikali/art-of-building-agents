package com.example.jarvis.constraints.llmjudge.suitability;

/** Possible outcomes of {@link MealSuitabilityCheck}. */
public enum MealSuitabilityStatus {
  /** Restaurant appears to fit the stated meal context. */
  PASS,
  /** Restaurant appears clearly unsuited to the stated meal context. */
  FAIL,
  /** Evidence is too weak or mixed to judge confidently. */
  UNSURE
}
