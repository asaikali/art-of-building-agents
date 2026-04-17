package com.example.jarvis.constraints.deterministic.travel;

public record TravelTimeCheckResult(
    TravelTimeCheckStatus status, String rationale, int estimatedMinutes) {}
