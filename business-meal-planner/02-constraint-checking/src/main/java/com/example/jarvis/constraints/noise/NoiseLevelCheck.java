package com.example.jarvis.constraints.noise;

import com.example.jarvis.requirements.NoiseLevel;
import com.example.restaurant.RestaurantService;
import org.springframework.stereotype.Component;

@Component
public class NoiseLevelCheck {

  private final RestaurantService restaurantService;

  public NoiseLevelCheck(RestaurantService restaurantService) {
    this.restaurantService = restaurantService;
  }

  public NoiseLevelCheckResult check(NoiseLevel requestedNoiseLevel, String restaurantId) {
    if (restaurantId == null || restaurantId.isBlank()) {
      throw new IllegalArgumentException("restaurantId must be provided");
    }

    if (requestedNoiseLevel == null) {
      return new NoiseLevelCheckResult(
          NoiseLevelCheckStatus.PASS, "No noise level preference was provided.");
    }

    var restaurant =
        restaurantService
            .findById(restaurantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown restaurant id: " + restaurantId));

    var actualNoiseLevel = NoiseLevel.fromString(restaurant.noiseLevel());

    if (actualNoiseLevel == null) {
      throw new IllegalStateException(
          "Restaurant noise level is missing or invalid for: " + restaurantId);
    }

    boolean passes = toRank(actualNoiseLevel) <= toRank(requestedNoiseLevel);

    return new NoiseLevelCheckResult(
        passes ? NoiseLevelCheckStatus.PASS : NoiseLevelCheckStatus.FAIL,
        "Requested %s, restaurant is %s."
            .formatted(requestedNoiseLevel.name(), actualNoiseLevel.name()));
  }

  private int toRank(NoiseLevel noiseLevel) {
    return switch (noiseLevel) {
      case QUIET -> 1;
      case MODERATE -> 2;
      case LOUD -> 3;
    };
  }
}
