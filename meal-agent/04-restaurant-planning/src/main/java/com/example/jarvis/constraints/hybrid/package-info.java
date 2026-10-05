/**
 * Hybrid constraint checks: Java gathers evidence deterministically, then the model judges whether
 * the evidence is good enough.
 *
 * <p>The Java half does the work that code is good at — filtering, normalizing, deduplicating,
 * loading data — so the model only sees a small, focused payload. The model half does the work that
 * code is bad at — making a qualitative call about whether the evidence really fits the user's
 * needs.
 *
 * <p>Hybrid checks also short-circuit before calling the model whenever the answer is already
 * obvious from Java (e.g. no constraints to check, or no data to evaluate), which keeps cost and
 * latency down and keeps the model focused on the cases that actually need judgment.
 */
package com.example.jarvis.constraints.hybrid;
