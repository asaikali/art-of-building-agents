package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class RestaurantAvailabilityServiceTest {

  static RestaurantAvailabilityService service;

  @BeforeAll
  static void setUp() {
    var restaurantService = new RestaurantService(new ObjectMapper());
    service = new RestaurantAvailabilityService(restaurantService);
  }

  @Test
  @DisplayName("Magic time 18:00 returns all restaurants (filtered by neighborhood if given)")
  void magicTimeAlwaysAvailable() {
    var result =
        service.findAvailableRestaurants(LocalDate.of(2026, 4, 20), LocalTime.of(18, 0), 2, null);

    assertThat(result).isNotEmpty();
    assertThat(result).hasSizeGreaterThanOrEqualTo(24);
  }

  @Test
  @DisplayName("Magic time 20:00 returns empty list")
  void magicTimeNeverAvailable() {
    var result =
        service.findAvailableRestaurants(LocalDate.of(2026, 4, 20), LocalTime.of(20, 0), 2, null);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Magic party size 2 with magic time 18:00 returns all restaurants")
  void magicPartySizeAlwaysAvailable() {
    var result =
        service.findAvailableRestaurants(LocalDate.of(2026, 4, 20), LocalTime.of(18, 0), 2, null);

    assertThat(result).hasSizeGreaterThanOrEqualTo(24);
  }

  @Test
  @DisplayName("Magic party size 3 returns empty even with magic time 18:00")
  void magicPartySizeNeverAvailable() {
    var result =
        service.findAvailableRestaurants(LocalDate.of(2026, 4, 20), LocalTime.of(18, 0), 3, null);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Non-magic values return a mix of restaurants (not all, not none)")
  void nonMagicValuesReturnMix() {
    var result =
        service.findAvailableRestaurants(LocalDate.of(2026, 4, 20), LocalTime.of(19, 0), 4, null);

    assertThat(result).isNotEmpty();
    assertThat(result.size()).isLessThan(24);
  }

  @Test
  @DisplayName("Non-magic values return stable results across calls")
  void nonMagicValuesAreStable() {
    var date = LocalDate.of(2026, 4, 20);
    var time = LocalTime.of(19, 0);

    var first = service.findAvailableRestaurants(date, time, 4, null);
    var second = service.findAvailableRestaurants(date, time, 4, null);

    assertThat(first).isEqualTo(second);
  }

  @Test
  @DisplayName("Neighborhood filter narrows results")
  void neighborhoodFilterNarrowsResults() {
    var all =
        service.findAvailableRestaurants(LocalDate.of(2026, 4, 20), LocalTime.of(18, 0), 2, null);
    var downtown =
        service.findAvailableRestaurants(
            LocalDate.of(2026, 4, 20), LocalTime.of(18, 0), 2, "Downtown");

    assertThat(downtown).isNotEmpty();
    assertThat(downtown.size()).isLessThan(all.size());
    assertThat(downtown).allMatch(r -> r.neighborhood().toLowerCase().contains("downtown"));
  }

  @Test
  @DisplayName("Results are sorted by restaurant name")
  void resultsSortedByName() {
    var result =
        service.findAvailableRestaurants(LocalDate.of(2026, 4, 20), LocalTime.of(18, 0), 2, null);

    assertThat(result).isSortedAccordingTo((a, b) -> a.name().compareTo(b.name()));
  }

  @Test
  @DisplayName("Throws when date is null")
  void throwsWhenDateIsNull() {
    assertThatThrownBy(() -> service.findAvailableRestaurants(null, LocalTime.of(18, 0), 4, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("date must be provided");
  }

  @Test
  @DisplayName("Throws when time is null")
  void throwsWhenTimeIsNull() {
    assertThatThrownBy(
            () -> service.findAvailableRestaurants(LocalDate.of(2026, 4, 20), null, 4, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("time must be provided");
  }

  @Test
  @DisplayName("Throws when party size is zero")
  void throwsWhenPartySizeIsZero() {
    assertThatThrownBy(
            () ->
                service.findAvailableRestaurants(
                    LocalDate.of(2026, 4, 20), LocalTime.of(18, 0), 0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("partySize must be greater than zero");
  }
}
