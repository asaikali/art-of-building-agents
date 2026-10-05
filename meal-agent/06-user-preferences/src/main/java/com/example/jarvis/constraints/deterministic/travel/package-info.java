/**
 * Travel-time constraint check, backed by a fake neighborhood-to-neighborhood matrix.
 *
 * <p>The design is deliberately split in two:
 *
 * <ul>
 *   <li>{@link com.example.jarvis.constraints.deterministic.travel.TravelTimeEstimatorService}
 *       looks up the baseline minutes from {@link com.example.restaurant.TravelTimeMatrix} and
 *       applies a per-mode adjustment.
 *   <li>{@link com.example.jarvis.constraints.deterministic.travel.TravelTimeCheck} compares the
 *       estimate to the attendee's {@code maxTravelTimeMinutes} and returns PASS, FAIL, or UNSURE.
 * </ul>
 *
 * <p>The estimator is fake on purpose: no geocoding, no maps API, no network calls. That keeps the
 * workshop deterministic, scenario-friendly, and free of API keys or flaky tests. The underlying
 * matrix data lives with the rest of the fake restaurant data in {@code
 * components/restaurant-data}.
 */
package com.example.jarvis.constraints.deterministic.travel;
