/**
 * Constraint checks implemented entirely in Java — no model is called.
 *
 * <p>Each check applies a clear comparison rule against fake restaurant data and returns a typed
 * status with a short rationale. Because the logic is pure code, the verdicts are reproducible from
 * the inputs alone and the checks can be tested without an API key.
 *
 * <ul>
 *   <li>{@link com.example.jarvis.constraints.deterministic.budget} — per-person budget vs the
 *       restaurant's published price range
 *   <li>{@link com.example.jarvis.constraints.deterministic.noise} — requested noise tolerance vs
 *       the restaurant's stored noise level
 *   <li>{@link com.example.jarvis.constraints.deterministic.travel} — estimated travel time vs the
 *       attendee's maximum allowed minutes
 * </ul>
 */
package com.example.jarvis.constraints.deterministic;
