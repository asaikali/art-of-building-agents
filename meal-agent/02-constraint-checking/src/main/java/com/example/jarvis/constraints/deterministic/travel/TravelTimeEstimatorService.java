package com.example.jarvis.constraints.deterministic.travel;

import com.example.jarvis.requirements.TravelMode;
import com.example.restaurant.TravelTimeMatrix;
import org.springframework.stereotype.Service;

/**
 * Estimates travel time in minutes between two neighborhoods for a given travel mode, using the
 * fake matrix loaded by {@link com.example.restaurant.TravelTimeMatrix}.
 *
 * <p>The estimate is computed in two steps: a baseline neighborhood-to-neighborhood lookup,
 * followed by a per-mode adjustment:
 *
 * <ul>
 *   <li>{@code WALKING} — base minutes
 *   <li>{@code TRANSIT} — {@code round(base * 1.3)}
 *   <li>{@code DRIVING} — {@code max(8, round(base * 0.9))}
 *   <li>{@code TAXI} — {@code max(8, round(base * 1.0))}
 * </ul>
 *
 * <p>These multipliers are not trying to model real Toronto traffic. They are tuned to be
 * predictable, visibly different per mode, and easy to reason about in workshop scenarios.
 *
 * <p>If the matrix has no entry for a neighborhood pair, the estimator returns a fixed fallback of
 * 60 minutes and intentionally <em>skips</em> the travel-mode adjustment — the fallback already
 * means "we don't really know", so layering arithmetic on top would be misleading.
 */
@Service
public class TravelTimeEstimatorService {

  private static final int FALLBACK_ESTIMATED_MINUTES = 60;

  private final TravelTimeMatrix travelTimeMatrix;

  public TravelTimeEstimatorService(TravelTimeMatrix travelTimeMatrix) {
    this.travelTimeMatrix = travelTimeMatrix;
  }

  public int estimateMinutes(
      String originNeighborhood, String restaurantNeighborhood, TravelMode travelMode) {
    validateInputs(originNeighborhood, restaurantNeighborhood, travelMode);

    return travelTimeMatrix
        .findBaseMinutes(originNeighborhood, restaurantNeighborhood)
        .map(baseMinutes -> applyTravelMode(baseMinutes, travelMode))
        .orElse(FALLBACK_ESTIMATED_MINUTES);
  }

  private void validateInputs(
      String originNeighborhood, String restaurantNeighborhood, TravelMode travelMode) {
    if (originNeighborhood == null || originNeighborhood.isBlank()) {
      throw new IllegalArgumentException("originNeighborhood must be provided");
    }
    if (restaurantNeighborhood == null || restaurantNeighborhood.isBlank()) {
      throw new IllegalArgumentException("restaurantNeighborhood must be provided");
    }
    if (travelMode == null) {
      throw new IllegalArgumentException("travelMode must be provided");
    }
  }

  private int applyTravelMode(int baseMinutes, TravelMode travelMode) {
    return switch (travelMode) {
      case WALKING -> baseMinutes;
      case TRANSIT -> (int) Math.round(baseMinutes * 1.3);
      case DRIVING -> Math.max(8, (int) Math.round(baseMinutes * 0.9));
      case TAXI -> Math.max(8, (int) Math.round(baseMinutes * 1.0));
    };
  }
}
