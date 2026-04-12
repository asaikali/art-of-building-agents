package com.example.jarvis.constraints.travel;

import com.example.jarvis.requirements.TravelMode;
import com.example.restaurant.RestaurantService;
import org.springframework.stereotype.Component;

@Component
public class TravelTimeCheck {

  private final TravelTimeEstimatorService estimator;
  private final RestaurantService restaurantService;

  public TravelTimeCheck(
      TravelTimeEstimatorService estimator, RestaurantService restaurantService) {
    this.estimator = estimator;
    this.restaurantService = restaurantService;
  }

  public TravelTimeCheckResult check(
      String originNeighborhood,
      TravelMode travelMode,
      Integer maxTravelTimeMinutes,
      String restaurantId) {
    if (restaurantId == null || restaurantId.isBlank()) {
      throw new IllegalArgumentException("restaurantId must be provided");
    }

    if (originNeighborhood == null
        || originNeighborhood.isBlank()
        || travelMode == null
        || maxTravelTimeMinutes == null) {
      return new TravelTimeCheckResult(
          TravelTimeCheckStatus.UNSURE, "Required travel inputs are missing.", 0);
    }

    var restaurant =
        restaurantService
            .findById(restaurantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown restaurant id: " + restaurantId));

    int estimated =
        estimator.estimateMinutes(originNeighborhood, restaurant.neighborhood(), travelMode);

    if (estimated <= maxTravelTimeMinutes) {
      return new TravelTimeCheckResult(
          TravelTimeCheckStatus.PASS,
          "Estimated %d minutes, within the %d minute limit."
              .formatted(estimated, maxTravelTimeMinutes),
          estimated);
    }

    return new TravelTimeCheckResult(
        TravelTimeCheckStatus.FAIL,
        "Estimated %d minutes, exceeds the %d minute limit."
            .formatted(estimated, maxTravelTimeMinutes),
        estimated);
  }
}
