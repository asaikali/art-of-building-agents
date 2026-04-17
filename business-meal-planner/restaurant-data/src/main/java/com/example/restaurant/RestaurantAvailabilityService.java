package com.example.restaurant;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Determines which restaurants are available for a given date, time, and party size.
 *
 * <p>Availability is computed, not stored. Two magic values provide deterministic behavior for
 * workshop scenarios:
 *
 * <ul>
 *   <li><b>Time 18:00</b> — always available (happy path)
 *   <li><b>Time 20:00</b> — never available (failure scenario)
 *   <li><b>Party size 2</b> — always available (happy path)
 *   <li><b>Party size 3</b> — never available (failure scenario)
 * </ul>
 *
 * <p>For non-magic values, availability is pseudo-random per restaurant+date, stable across calls.
 */
@Service
public class RestaurantAvailabilityService {

  private static final LocalTime ALWAYS_AVAILABLE_TIME = LocalTime.of(18, 0);
  private static final LocalTime NEVER_AVAILABLE_TIME = LocalTime.of(20, 0);
  private static final int ALWAYS_AVAILABLE_PARTY_SIZE = 2;
  private static final int NEVER_AVAILABLE_PARTY_SIZE = 3;

  private final RestaurantService restaurantService;

  public RestaurantAvailabilityService(RestaurantService restaurantService) {
    this.restaurantService = restaurantService;
  }

  public List<Restaurant> findAvailableRestaurants(
      LocalDate date, LocalTime time, int partySize, String neighborhood) {
    if (date == null) {
      throw new IllegalArgumentException("date must be provided");
    }
    if (time == null) {
      throw new IllegalArgumentException("time must be provided");
    }
    if (partySize <= 0) {
      throw new IllegalArgumentException("partySize must be greater than zero");
    }

    return restaurantService.findAll().stream()
        .filter(r -> matchesNeighborhood(r, neighborhood))
        .filter(r -> isAvailable(r.id(), date, time, partySize))
        .sorted(Comparator.comparing(Restaurant::name))
        .toList();
  }

  boolean isAvailable(String restaurantId, LocalDate date, LocalTime time, int partySize) {
    return isTimeAvailable(restaurantId, date, time)
        && isPartySizeAvailable(restaurantId, date, partySize);
  }

  private boolean isTimeAvailable(String restaurantId, LocalDate date, LocalTime time) {
    if (ALWAYS_AVAILABLE_TIME.equals(time)) {
      return true;
    }
    if (NEVER_AVAILABLE_TIME.equals(time)) {
      return false;
    }
    return pseudoRandom(restaurantId, date);
  }

  private boolean isPartySizeAvailable(String restaurantId, LocalDate date, int partySize) {
    if (partySize == ALWAYS_AVAILABLE_PARTY_SIZE) {
      return true;
    }
    if (partySize == NEVER_AVAILABLE_PARTY_SIZE) {
      return false;
    }
    return pseudoRandom(restaurantId, date);
  }

  private boolean pseudoRandom(String restaurantId, LocalDate date) {
    int hash = (restaurantId + date.toString()).hashCode();
    return (hash & 1) == 0;
  }

  private boolean matchesNeighborhood(Restaurant restaurant, String neighborhood) {
    if (neighborhood == null || neighborhood.isBlank()) {
      return true;
    }
    return restaurant
        .neighborhood()
        .toLowerCase(Locale.ROOT)
        .contains(neighborhood.strip().toLowerCase(Locale.ROOT));
  }
}
