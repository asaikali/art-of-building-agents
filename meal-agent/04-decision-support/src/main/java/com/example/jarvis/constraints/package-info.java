/**
 * Phase 2 constraint checks: given confirmed requirements and one restaurant candidate, run every
 * check and report what each one said.
 *
 * <p>The entry point is {@link
 * com.example.jarvis.constraints.RestaurantCandidateCheckService#check}, which returns a strongly
 * typed {@link com.example.jarvis.constraints.RestaurantCheckResult} aggregating one verdict per
 * underlying check.
 *
 * <p>Checks are organized by how the verdict is reached:
 *
 * <ul>
 *   <li>{@link com.example.jarvis.constraints.deterministic} — pure Java comparison rules
 *   <li>{@link com.example.jarvis.constraints.hybrid} — Java gathers evidence, the model judges
 *   <li>{@link com.example.jarvis.constraints.llmjudge} — the model judges from metadata alone
 * </ul>
 *
 * <p>This package is orchestration only. It does not score, rank, classify checks as hard vs soft,
 * or filter candidates. Those interpretations belong to a higher layer (Phase 3 restaurant
 * planning), which is free to read the typed results however it needs to.
 */
package com.example.jarvis.constraints;
