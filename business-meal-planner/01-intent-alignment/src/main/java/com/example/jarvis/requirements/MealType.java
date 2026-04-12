package com.example.jarvis.requirements;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MealType {
  BREAKFAST,
  BRUNCH,
  LUNCH,
  DINNER;

  // LLMs sometimes return "" instead of null for unknown enum fields.
  @JsonCreator
  public static MealType fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return valueOf(value.trim().toUpperCase());
  }
}
