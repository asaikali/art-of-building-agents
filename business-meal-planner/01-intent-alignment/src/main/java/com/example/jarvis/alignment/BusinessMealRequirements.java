package com.example.jarvis.alignment;

import java.util.List;

public record BusinessMealRequirements(
    String intent,
    List<String> explicitConstraints,
    List<String> inferredConstraints,
    List<String> missingInformation,
    List<String> assumptions,
    RequirementStatus status) {

  public BusinessMealRequirements {
    intent = normalizeIntent(intent);
    explicitConstraints = sanitize(explicitConstraints);
    inferredConstraints = sanitize(inferredConstraints);
    missingInformation = sanitize(missingInformation);
    assumptions = sanitize(assumptions);
  }

  public BusinessMealRequirements withStatus(RequirementStatus nextStatus) {
    return new BusinessMealRequirements(
        intent,
        explicitConstraints,
        inferredConstraints,
        missingInformation,
        assumptions,
        nextStatus);
  }

  public static BusinessMealRequirements fromDraft(
      BusinessMealRequirementsDraft draft, RequirementStatus status) {
    return new BusinessMealRequirements(
        draft.intent(),
        draft.explicitConstraints(),
        draft.inferredConstraints(),
        draft.missingInformation(),
        draft.assumptions(),
        status);
  }

  private static String normalizeIntent(String value) {
    if (value == null || value.isBlank()) {
      return "Clarify the business meal request.";
    }
    return value.trim();
  }

  private static List<String> sanitize(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .toList();
  }
}
