package com.example.agents.a2a.client;

import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Restaurant tools — search, dietary check, and booking. Expense policy checking is handled by the
 * remote A2A agent instead of a local tool.
 */
@Service
public class RestaurantTools {

  private static final List<Map<String, Object>> RESTAURANTS =
      List.of(
          Map.of(
              "id", "rest-001",
              "name", "Can Culleretes",
              "neighborhood", "Gothic Quarter",
              "cuisine", "Catalan",
              "pricePerPerson", 35,
              "vegetarianOptions", true,
              "noiseLevel", "moderate"),
          Map.of(
              "id", "rest-002",
              "name", "Cervecería Catalana",
              "neighborhood", "Eixample",
              "cuisine", "Tapas",
              "pricePerPerson", 28,
              "vegetarianOptions", true,
              "noiseLevel", "lively"),
          Map.of(
              "id", "rest-003",
              "name", "El Nacional",
              "neighborhood", "Passeig de Gràcia",
              "cuisine", "Mediterranean",
              "pricePerPerson", 55,
              "vegetarianOptions", true,
              "noiseLevel", "moderate"),
          Map.of(
              "id", "rest-004",
              "name", "Tickets Bar",
              "neighborhood", "Paral·lel",
              "cuisine", "Modern Spanish",
              "pricePerPerson", 75,
              "vegetarianOptions", false,
              "noiseLevel", "lively"),
          Map.of(
              "id", "rest-005",
              "name", "Teresa Carles",
              "neighborhood", "Eixample",
              "cuisine", "Vegetarian",
              "pricePerPerson", 22,
              "vegetarianOptions", true,
              "noiseLevel", "quiet"));

  @Tool(description = "Search for restaurants in Barcelona by neighborhood or cuisine type")
  public List<Map<String, Object>> searchRestaurants(
      @ToolParam(description = "Neighborhood, area, or cuisine to search for") String query) {
    return RESTAURANTS.stream()
        .filter(
            r ->
                r.get("neighborhood").toString().toLowerCase().contains(query.toLowerCase())
                    || r.get("cuisine").toString().toLowerCase().contains(query.toLowerCase()))
        .toList();
  }

  @Tool(description = "Check if a restaurant has menu options for specific dietary requirements")
  public Map<String, Object> checkDietaryOptions(
      @ToolParam(description = "Restaurant name") String restaurantName,
      @ToolParam(description = "Dietary requirement, e.g. 'vegetarian', 'vegan', 'gluten-free'")
          String dietaryRequirement) {
    var restaurant =
        RESTAURANTS.stream()
            .filter(
                r -> r.get("name").toString().toLowerCase().contains(restaurantName.toLowerCase()))
            .findFirst();

    if (restaurant.isEmpty()) {
      return Map.of("found", false, "message", "Restaurant not found: " + restaurantName);
    }

    boolean hasOptions =
        "vegetarian".equalsIgnoreCase(dietaryRequirement)
            && (boolean) restaurant.get().get("vegetarianOptions");

    return Map.of(
        "found",
        true,
        "restaurant",
        restaurant.get().get("name"),
        "requirement",
        dietaryRequirement,
        "hasOptions",
        hasOptions);
  }

  @Tool(description = "Book a table at a restaurant for a given date, time, and party size")
  public Map<String, Object> bookTable(
      @ToolParam(description = "Restaurant name") String restaurantName,
      @ToolParam(description = "Number of guests") int partySize,
      @ToolParam(description = "Date in YYYY-MM-DD format") String date,
      @ToolParam(description = "Time in HH:MM format") String time) {
    return Map.of(
        "confirmed", true,
        "restaurant", restaurantName,
        "partySize", partySize,
        "date", date,
        "time", time,
        "confirmationCode", "BCN-" + System.currentTimeMillis() % 10000);
  }
}
