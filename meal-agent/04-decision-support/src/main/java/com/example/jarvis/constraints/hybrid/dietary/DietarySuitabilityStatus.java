package com.example.jarvis.constraints.hybrid.dietary;

/** Possible outcomes of {@link DietarySuitabilityCheck}. */
public enum DietarySuitabilityStatus {
  /** Menu appears to provide meaningful options for the dietary needs. */
  PASS,
  /** Menu appears clearly unsuitable for one or more dietary needs. */
  FAIL,
  /** Menu evidence is too thin or ambiguous to judge confidently. */
  UNSURE
}
