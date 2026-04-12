package com.example.jarvis.requirements;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum NoiseLevel {
  QUIET,
  MODERATE,
  LIVELY;

  // LLMs sometimes return "" instead of null for unknown enum fields.
  @JsonCreator
  public static NoiseLevel fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return valueOf(value.trim().toUpperCase());
  }
}
