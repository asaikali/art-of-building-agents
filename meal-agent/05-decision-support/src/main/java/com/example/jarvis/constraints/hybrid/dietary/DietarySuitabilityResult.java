package com.example.jarvis.constraints.hybrid.dietary;

/** Verdict from {@link DietarySuitabilityCheck} with a short rationale. */
public record DietarySuitabilityResult(DietarySuitabilityStatus status, String rationale) {}
