package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RestaurantServiceTest {

  static RestaurantService service;

  @BeforeAll
  static void setUp() {
    service = new RestaurantService(new ObjectMapper());
  }

  @Test
  void loadsAllRestaurants() {
    var all = service.findAll();
    assertThat(all).isNotEmpty();
    assertThat(all).hasSizeGreaterThanOrEqualTo(24);
  }

  @Test
  void everyRestaurantHasRequiredFields() {
    for (var r : service.findAll()) {
      assertThat(r.id()).as("id for %s", r.name()).isNotBlank();
      assertThat(r.name()).as("name for %s", r.id()).isNotBlank();
      assertThat(r.address()).as("address for %s", r.id()).isNotBlank();
      assertThat(r.neighborhood()).as("neighborhood for %s", r.id()).isNotBlank();
    }
  }

  @Test
  void findByIdReturnsKnownRestaurant() {
    var result = service.findById("canoe");
    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("Canoe Restaurant");
  }

  @Test
  void findByIdReturnsEmptyForUnknown() {
    assertThat(service.findById("does-not-exist")).isEmpty();
  }
}
