package com.example.jarvis.requirements;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DietaryConstraint {
  VEGETARIAN,
  VEGAN,
  GLUTEN_FREE,
  HALAL,
  KOSHER,
  NONE,
  OTHER;

  // LLMs sometimes return "" instead of null for unknown enum fields.
  @JsonCreator
  public static DietaryConstraint fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return valueOf(value.trim().toUpperCase());
  }
}
