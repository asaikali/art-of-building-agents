package com.example.jarvis.constraints.deterministic.noise;

/** Possible outcomes of {@link NoiseLevelCheck}. */
public enum NoiseLevelCheckStatus {
  /** Restaurant is at or below the requested noise tolerance. */
  PASS,
  /** Restaurant is louder than the requested noise tolerance. */
  FAIL
}
