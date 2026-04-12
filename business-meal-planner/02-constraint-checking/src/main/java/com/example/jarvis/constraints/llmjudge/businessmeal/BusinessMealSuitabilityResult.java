package com.example.jarvis.constraints.llmjudge.businessmeal;

public record BusinessMealSuitabilityResult(
    BusinessMealSuitabilityStatus status, String rationale) {}
