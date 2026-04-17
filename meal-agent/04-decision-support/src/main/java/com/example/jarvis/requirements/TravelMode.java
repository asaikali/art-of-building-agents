package com.example.jarvis.requirements;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TravelMode {
  WALKING,
  DRIVING,
  TRANSIT,
  TAXI;

  // LLMs sometimes return "" instead of null for unknown enum fields.
  // Jackson 3 rejects empty strings for enums by default, so this
  // factory method treats blank values as null.
  @JsonCreator
  public static TravelMode fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return valueOf(value.trim().toUpperCase());
  }
}
