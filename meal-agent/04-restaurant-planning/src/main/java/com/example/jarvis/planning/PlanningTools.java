package com.example.jarvis.planning;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.constraints.RestaurantCandidate;
import com.example.jarvis.constraints.RestaurantCandidateCheckService;
import com.example.jarvis.constraints.RestaurantCheckResult;
import com.example.jarvis.requirements.UserRequirements;
import com.example.restaurant.Restaurant;
import com.example.restaurant.RestaurantAvailabilityService;
import com.example.restaurant.RestaurantService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class PlanningTools {

  private static final Logger log = LoggerFactory.getLogger(PlanningTools.class);

  private final RestaurantAvailabilityService availabilityService;
  private final RestaurantService restaurantService;
  private final RestaurantCandidateCheckService checkService;

  // Set by the handler before each planning call. Not thread-safe — fine for a workshop.
  private UserRequirements currentRequirements;

  public PlanningTools(
      RestaurantAvailabilityService availabilityService,
      RestaurantService restaurantService,
      RestaurantCandidateCheckService checkService) {
    this.availabilityService = availabilityService;
    this.restaurantService = restaurantService;
    this.checkService = checkService;
  }

  public void setCurrentRequirements(UserRequirements requirements) {
    this.currentRequirements = requirements;
  }

  @Tool(
      description =
          """
      Search for restaurants available on a specific date, time, and party size.
      Optionally filter by neighborhood. Returns a list of available restaurants.""")
  public String findAvailableRestaurants(
      @ToolParam(description = "Date in ISO format, e.g. 2026-04-20") String date,
      @ToolParam(description = "Time in HH:MM format, e.g. 18:00") String time,
      @ToolParam(description = "Number of people in the party") int partySize,
      @ToolParam(description = "Optional neighborhood to filter by, or null for all areas")
          String neighborhood) {

    log.info(
        "findAvailableRestaurants | date={} time={} partySize={} neighborhood={}",
        date,
        time,
        partySize,
        neighborhood);

    List<Restaurant> available =
        availabilityService.findAvailableRestaurants(
            LocalDate.parse(date), LocalTime.parse(time), partySize, neighborhood);

    var summary =
        available.stream()
            .map(
                r ->
                    new RestaurantSummary(
                        r.id(), r.name(), r.neighborhood(), r.priceRangePerPerson().label()))
            .toList();

    log.info("findAvailableRestaurants | found {} restaurants", summary.size());
    return JsonUtils.toJson(summary);
  }

  @Tool(
      description =
          """
      Get detailed information about a specific restaurant including description,
      noise level, price range, and neighborhood.""")
  public String getRestaurantDetails(
      @ToolParam(description = "The restaurant ID to look up") String restaurantId) {

    log.info("getRestaurantDetails | restaurantId={}", restaurantId);

    var restaurant =
        restaurantService
            .findById(restaurantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown restaurant id: " + restaurantId));

    return JsonUtils.toJson(restaurant);
  }

  @Tool(
      description =
          """
      Run all constraint checks against a restaurant candidate. Returns results for
      noise level, budget, travel time, dietary suitability, and meal suitability.
      FAIL = hard violation (deal-breaker). MAYBE/UNSURE = soft (worth noting).
      PASS = constraint satisfied.""")
  public String checkRestaurantCandidate(
      @ToolParam(description = "The restaurant ID to evaluate") String restaurantId) {

    log.info("checkRestaurantCandidate | restaurantId={}", restaurantId);

    if (currentRequirements == null) {
      throw new IllegalStateException(
          "UserRequirements must be set before calling checkRestaurantCandidate");
    }

    var restaurant =
        restaurantService
            .findById(restaurantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown restaurant id: " + restaurantId));

    var candidate = new RestaurantCandidate(restaurant.id(), restaurant.name());
    RestaurantCheckResult result = checkService.check(currentRequirements, candidate);

    log.info("checkRestaurantCandidate | result={}", JsonUtils.toJson(result));
    return JsonUtils.toJson(result);
  }

  private record RestaurantSummary(
      String id, String name, String neighborhood, String priceRange) {}
}
