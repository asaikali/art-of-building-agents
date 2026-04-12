package com.example.jarvis.constraints.travel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jarvis.requirements.TravelMode;
import com.example.restaurant.TravelTimeMatrix;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TravelTimeEstimatorServiceTest {

  private TravelTimeEstimatorService service;

  @BeforeEach
  void setUp() {
    var travelTimeMatrix = new TravelTimeMatrix(new ObjectMapper());
    service = new TravelTimeEstimatorService(travelTimeMatrix);
  }

  @Test
  @DisplayName("Returns same-neighborhood walking estimate from the matrix")
  void returnsWalkingEstimateForSameNeighborhood() {
    int result =
        service.estimateMinutes("Financial District", "Financial District", TravelMode.WALKING);

    assertThat(result).isEqualTo(10);
  }

  @Test
  @DisplayName("Applies transit adjustment to the base travel time")
  void appliesTransitAdjustment() {
    int result = service.estimateMinutes("Yorkville", "Koreatown", TravelMode.TRANSIT);

    assertThat(result).isEqualTo(22);
  }

  @Test
  @DisplayName("Applies driving adjustment to the base travel time")
  void appliesDrivingAdjustment() {
    int result =
        service.estimateMinutes("Harbourfront", "Entertainment District", TravelMode.DRIVING);

    assertThat(result).isEqualTo(11);
  }

  @Test
  @DisplayName("Returns fallback estimate when the neighborhood pair is missing")
  void returnsFallbackWhenNeighborhoodPairIsMissing() {
    int result = service.estimateMinutes("Unknown", "Downtown", TravelMode.TRANSIT);

    assertThat(result).isEqualTo(60);
  }

  @Test
  @DisplayName("Throws when origin neighborhood is null")
  void throwsWhenOriginNeighborhoodIsNull() {
    assertThatThrownBy(() -> service.estimateMinutes(null, "Downtown", TravelMode.WALKING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("originNeighborhood must be provided");
  }

  @Test
  @DisplayName("Throws when origin neighborhood is blank")
  void throwsWhenOriginNeighborhoodIsBlank() {
    assertThatThrownBy(() -> service.estimateMinutes("  ", "Downtown", TravelMode.WALKING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("originNeighborhood must be provided");
  }

  @Test
  @DisplayName("Throws when restaurant neighborhood is null")
  void throwsWhenRestaurantNeighborhoodIsNull() {
    assertThatThrownBy(
            () -> service.estimateMinutes("Financial District", null, TravelMode.WALKING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantNeighborhood must be provided");
  }

  @Test
  @DisplayName("Throws when restaurant neighborhood is blank")
  void throwsWhenRestaurantNeighborhoodIsBlank() {
    assertThatThrownBy(() -> service.estimateMinutes("Financial District", " ", TravelMode.WALKING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantNeighborhood must be provided");
  }

  @Test
  @DisplayName("Throws when travel mode is null")
  void throwsWhenTravelModeIsNull() {
    assertThatThrownBy(() -> service.estimateMinutes("Financial District", "Downtown", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("travelMode must be provided");
  }
}
