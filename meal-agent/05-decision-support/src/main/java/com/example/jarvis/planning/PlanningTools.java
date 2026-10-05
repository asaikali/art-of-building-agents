package com.example.jarvis.planning;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.constraints.RestaurantCandidate;
import com.example.jarvis.constraints.RestaurantCandidateCheckService;
import com.example.jarvis.constraints.RestaurantCheckResult;
import com.example.jarvis.requirements.UserRequirements;
import com.example.restaurant.MenuService;
import com.example.restaurant.Restaurant;
import com.example.restaurant.RestaurantAvailabilityService;
import com.example.restaurant.RestaurantService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
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
  private final MenuService menuService;
  private final RestaurantCandidateCheckService checkService;

  // Set by the handler before each planning call. Not thread-safe — fine for a workshop.
  private UserRequirements currentRequirements;

  public PlanningTools(
      RestaurantAvailabilityService availabilityService,
      RestaurantService restaurantService,
      MenuService menuService,
      RestaurantCandidateCheckService checkService) {
    this.availabilityService = availabilityService;
    this.restaurantService = restaurantService;
    this.menuService = menuService;
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
      @ToolParam(description = "The restaurant ID or name") String restaurantId) {

    var resolvedId = resolveRestaurantId(restaurantId);
    log.info("getRestaurantDetails | input={} resolvedId={}", restaurantId, resolvedId);

    var restaurant =
        restaurantService
            .findById(resolvedId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown restaurant: " + restaurantId));

    return JsonUtils.toJson(restaurant);
  }

  @Tool(
      description =
          """
      Get the full menu for a restaurant, including all sections, items, prices,
      and dietary tags. Use this when the user asks about menu options, specific
      dishes, or dietary suitability details.""")
  public String getRestaurantMenu(
      @ToolParam(description = "The restaurant ID or name") String restaurantId) {

    var resolvedId = resolveRestaurantId(restaurantId);
    log.info("getRestaurantMenu | input={} resolvedId={}", restaurantId, resolvedId);

    var menu =
        menuService
            .findById(resolvedId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No menu data available for restaurant: " + restaurantId));

    return JsonUtils.toJson(menu);
  }

  @Tool(
      description =
          """
      Run all constraint checks against a restaurant candidate. Returns results for
      noise level, budget, travel time, dietary suitability, and meal suitability.
      FAIL = hard violation (deal-breaker). MAYBE/UNSURE = soft (worth noting).
      PASS = constraint satisfied.""")
  public String checkRestaurantCandidate(
      @ToolParam(description = "The restaurant ID or name") String restaurantId) {

    var resolvedId = resolveRestaurantId(restaurantId);
    log.info("checkRestaurantCandidate | input={} resolvedId={}", restaurantId, resolvedId);

    if (currentRequirements == null) {
      throw new IllegalStateException(
          "UserRequirements must be set before calling checkRestaurantCandidate");
    }

    var restaurant =
        restaurantService
            .findById(resolvedId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown restaurant: " + restaurantId));

    var candidate = new RestaurantCandidate(restaurant.id(), restaurant.name());
    RestaurantCheckResult result = checkService.check(currentRequirements, candidate);

    log.info("checkRestaurantCandidate | result={}", JsonUtils.toJson(result));
    return JsonUtils.toJson(result);
  }

  private String resolveRestaurantId(String restaurantIdOrName) {
    // Try exact ID match first
    if (restaurantService.findById(restaurantIdOrName).isPresent()) {
      return restaurantIdOrName;
    }
    // Fall back to name match
    var lower = restaurantIdOrName.toLowerCase(Locale.ROOT);
    return restaurantService.findAll().stream()
        .filter(r -> r.name().toLowerCase(Locale.ROOT).contains(lower))
        .map(Restaurant::id)
        .findFirst()
        .orElse(restaurantIdOrName);
  }

  private record RestaurantSummary(
      String id, String name, String neighborhood, String priceRange) {}
}
