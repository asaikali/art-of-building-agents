package com.example.jarvis.constraints.travel;

import com.example.jarvis.requirements.TravelMode;
import com.example.restaurant.TravelTimeMatrix;
import org.springframework.stereotype.Service;

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
