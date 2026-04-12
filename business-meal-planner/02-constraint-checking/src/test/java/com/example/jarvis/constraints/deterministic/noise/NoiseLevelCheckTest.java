package com.example.jarvis.constraints.deterministic.noise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jarvis.requirements.NoiseLevel;
import com.example.restaurant.RestaurantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoiseLevelCheckTest {

  private NoiseLevelCheck check;

  @BeforeEach
  void setUp() {
    var restaurantService = new RestaurantService(new ObjectMapper());
    check = new NoiseLevelCheck(restaurantService);
  }

  @Test
  @DisplayName("Returns PASS when no noise preference is provided")
  void returnsPassWhenNoNoisePreferenceIsProvided() {
    var result = check.check(null, "canoe");

    assertThat(result.status()).isEqualTo(NoiseLevelCheckStatus.PASS);
  }

  @Test
  @DisplayName("Returns PASS when quiet is requested and restaurant is quiet")
  void returnsPassForQuietRestaurant() {
    var result = check.check(NoiseLevel.QUIET, "canoe");

    assertThat(result.status()).isEqualTo(NoiseLevelCheckStatus.PASS);
  }

  @Test
  @DisplayName("Returns FAIL when quiet is requested and restaurant is loud")
  void returnsFailForLoudRestaurantWhenQuietIsRequested() {
    var result = check.check(NoiseLevel.QUIET, "baro");

    assertThat(result.status()).isEqualTo(NoiseLevelCheckStatus.FAIL);
  }

  @Test
  @DisplayName("Returns PASS when moderate is requested and restaurant is quiet")
  void returnsPassForQuietRestaurantWhenModerateIsRequested() {
    var result = check.check(NoiseLevel.MODERATE, "canoe");

    assertThat(result.status()).isEqualTo(NoiseLevelCheckStatus.PASS);
  }

  @Test
  @DisplayName("Throws when restaurant id is blank")
  void throwsWhenRestaurantIdIsBlank() {
    assertThatThrownBy(() -> check.check(NoiseLevel.QUIET, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantId must be provided");
  }
}
