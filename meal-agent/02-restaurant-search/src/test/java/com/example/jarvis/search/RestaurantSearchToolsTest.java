package com.example.jarvis.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.restaurant.RestaurantAvailabilityService;
import com.example.restaurant.RestaurantService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class RestaurantSearchToolsTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final RestaurantService restaurants = new RestaurantService(mapper);
  private final RestaurantSearchTools tools =
      new RestaurantSearchTools(new RestaurantAvailabilityService(restaurants));

  @Test
  void returnsAvailableCandidatesWithoutClaimingConstraintResults() {
    var options =
        mapper.readValue(
            tools.findAvailableRestaurants("2026-10-20", "18:00", 2, null),
            RestaurantSearchTools.RestaurantOption[].class);

    assertThat(options).hasSize(restaurants.findAll().size());
    assertThat(options).anyMatch(option -> option.id().equals("canoe"));
    var payload = mapper.readTree(tools.findAvailableRestaurants("2026-10-20", "18:00", 2, null));
    assertThat(payload.get(0).size()).isEqualTo(3);
    assertThat(payload.get(0).has("id")).isTrue();
    assertThat(payload.get(0).has("name")).isTrue();
    assertThat(payload.get(0).has("neighborhood")).isTrue();
  }

  @Test
  void searchesOnlyTheRequestedNeighborhood() {
    var neighborhood = restaurants.findById("canoe").orElseThrow().neighborhood();
    var options =
        mapper.readValue(
            tools.findAvailableRestaurants("2026-10-20", "18:00", 2, neighborhood),
            RestaurantSearchTools.RestaurantOption[].class);

    assertThat(options).isNotEmpty();
    assertThat(options).allMatch(option -> option.neighborhood().equals(neighborhood));
    assertThat(options).hasSizeLessThan(restaurants.findAll().size());
  }

  @Test
  void returnsNoCandidatesWhenTheRequestedTimeIsUnavailable() {
    assertThat(
            mapper.readValue(
                tools.findAvailableRestaurants("2026-10-20", "20:00", 2, null),
                RestaurantSearchTools.RestaurantOption[].class))
        .isEmpty();
  }
}
