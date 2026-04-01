package com.example.agents.toolcalling;

import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Restaurant tools for the Jarvis dinner planning agent.
 *
 * <p>Spring AI automatically discovers @Tool methods and exposes their descriptions to the LLM. You
 * don't need to manually register tools or write JSON schemas — the framework handles it.
 */
@Service
public class RestaurantTools {

  // Simulated restaurant data — in production this would query a real database or API
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
      @ToolParam(
              description = "Neighborhood or area to search in, e.g. 'Eixample', 'Gothic Quarter'")
          String neighborhood) {
    return RESTAURANTS.stream()
        .filter(
            r ->
                r.get("neighborhood").toString().toLowerCase().contains(neighborhood.toLowerCase())
                    || r.get("cuisine")
                        .toString()
                        .toLowerCase()
                        .contains(neighborhood.toLowerCase()))
        .toList();
  }
}
