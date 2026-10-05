package com.example.foundations.advisors;

import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {

  @Tool(
      description =
          "Find activities from a simulated catalog that suit a city's current weather. First call getCurrentWeather and use its returned weatherCondition and temperature; do not guess these values.")
  public ActivityResponse findActivities(
      @ToolParam(description = "City name from the weather result") String city,
      @ToolParam(description = "weatherCondition returned by getCurrentWeather")
          String weatherCondition,
      @ToolParam(description = "temperature in Celsius returned by getCurrentWeather")
          double temperature) {
    List<String> activities =
        switch (weatherCondition) {
          case "Snowy" -> List.of("Indoor ice skating", "Museum visit");
          case "Rainy" -> List.of("Art gallery visit", "Cafe tasting tour");
          default ->
              temperature >= 25
                  ? List.of("Shaded park walk", "Ice cream tasting tour")
                  : List.of("Self-guided walking tour", "Picnic in a park");
        };
    return new ActivityResponse(city, activities);
  }
}
