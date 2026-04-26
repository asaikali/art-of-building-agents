package com.example.jarvis.constraints.llmjudge.suitability;

/** Verdict from {@link MealSuitabilityCheck} with a short rationale. */
public record MealSuitabilityResult(MealSuitabilityStatus status, String rationale) {}
