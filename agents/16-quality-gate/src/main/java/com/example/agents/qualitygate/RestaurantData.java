package com.example.agents.qualitygate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Shared restaurant data and lookup for the quality gate workflow. */
public final class RestaurantData {

  private RestaurantData() {}

  static final double EXPENSE_LIMIT = 50.0;

  static final List<Map<String, Object>> RESTAURANTS =
      List.of(
          Map.of(
              "name", "Can Culleretes",
              "neighborhood", "Gothic Quarter",
              "cuisine", "Catalan",
              "pricePerPerson", 35,
              "vegetarianOptions", true,
              "noiseLevel", "moderate"),
          Map.of(
              "name", "Cervecería Catalana",
              "neighborhood", "Eixample",
              "cuisine", "Tapas",
              "pricePerPerson", 28,
              "vegetarianOptions", true,
              "noiseLevel", "lively"),
          Map.of(
              "name", "El Nacional",
              "neighborhood", "Passeig de Gràcia",
              "cuisine", "Mediterranean",
              "pricePerPerson", 55,
              "vegetarianOptions", true,
              "noiseLevel", "moderate"),
          Map.of(
              "name", "Tickets Bar",
              "neighborhood", "Paral·lel",
              "cuisine", "Modern Spanish",
              "pricePerPerson", 75,
              "vegetarianOptions", false,
              "noiseLevel", "lively"),
          Map.of(
              "name", "Teresa Carles",
              "neighborhood", "Eixample",
              "cuisine", "Vegetarian",
              "pricePerPerson", 22,
              "vegetarianOptions", true,
              "noiseLevel", "quiet"));

  /** Format all restaurant data as a text block for inclusion in prompts. */
  static String formatForPrompt() {
    return RESTAURANTS.stream()
        .map(
            r ->
                String.format(
                    "- %s (%s, %s, \u20ac%d/person, vegetarian: %s, noise: %s)",
                    r.get("name"),
                    r.get("neighborhood"),
                    r.get("cuisine"),
                    r.get("pricePerPerson"),
                    r.get("vegetarianOptions"),
                    r.get("noiseLevel")))
        .collect(Collectors.joining("\n"));
  }
}
