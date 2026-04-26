package com.example.jarvis.constraints.deterministic.travel;

/** Verdict from {@link TravelTimeCheck} with the estimated minutes and a short rationale. */
public record TravelTimeCheckResult(
    TravelTimeCheckStatus status, String rationale, int estimatedMinutes) {}
