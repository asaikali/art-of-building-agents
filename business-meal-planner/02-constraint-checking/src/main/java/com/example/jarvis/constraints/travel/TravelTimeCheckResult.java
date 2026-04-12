package com.example.jarvis.constraints.travel;

public record TravelTimeCheckResult(
    TravelTimeCheckStatus status, String rationale, int estimatedMinutes) {}
