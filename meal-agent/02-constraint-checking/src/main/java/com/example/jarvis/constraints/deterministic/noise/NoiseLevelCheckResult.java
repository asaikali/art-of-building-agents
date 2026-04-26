package com.example.jarvis.constraints.deterministic.noise;

/** Verdict from {@link NoiseLevelCheck} with a short rationale. */
public record NoiseLevelCheckResult(NoiseLevelCheckStatus status, String rationale) {}
