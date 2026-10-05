package com.example.jarvis.search;

import com.example.agent.core.json.JsonUtils;
import com.example.restaurant.RestaurantAvailabilityService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** Searches availability; the result supplies no evidence about the other meal requirements. */
@Service
public class RestaurantSearchTools {

  private final RestaurantAvailabilityService availabilityService;

  public RestaurantSearchTools(RestaurantAvailabilityService availabilityService) {
    this.availabilityService = availabilityService;
  }

  @Tool(
      description =
          """
      Find restaurants available on a date and time for a party size.
      Optionally filter by neighborhood. Returns restaurant IDs, names, and neighborhoods.
      Availability does not establish budget, noise, dietary, travel, or occasion suitability.
      """)
  public String findAvailableRestaurants(
      @ToolParam(description = "Date in ISO format, for example 2026-10-20") String date,
      @ToolParam(description = "Time in HH:mm format, for example 18:00") String time,
      @ToolParam(description = "Number of people in the party") int partySize,
      @ToolParam(
              required = false,
              description = "Optional neighborhood, or null to search all neighborhoods")
          String neighborhood) {
    var restaurants =
        availabilityService.findAvailableRestaurants(
            LocalDate.parse(date), LocalTime.parse(time), partySize, neighborhood);
    var options =
        restaurants.stream()
            .map(r -> new RestaurantOption(r.id(), r.name(), r.neighborhood()))
            .toList();
    return JsonUtils.toJson(options);
  }

  public record RestaurantOption(String id, String name, String neighborhood) {}
}
