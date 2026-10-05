package com.example.foundations.advisors;

import java.time.LocalDateTime;
import java.util.Random;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

  private static final String[] CONDITIONS = {"Sunny", "Cloudy", "Rainy", "Partly Cloudy", "Snowy"};
  private static final String[] WIND_DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

  @Tool(
      description =
          "Get the current weather for a city, including temperature in Celsius, conditions, humidity, and wind in km/h")
  public WeatherResponse getCurrentWeather(
      @ToolParam(description = "City name, for example Toronto or Paris") String city) {
    Random random = new Random();
    String condition = CONDITIONS[random.nextInt(CONDITIONS.length)];
    double temperature =
        switch (condition) {
          case "Snowy" -> -10 + 10 * random.nextDouble();
          case "Rainy" -> 5 + 15 * random.nextDouble();
          default -> 10 + 20 * random.nextDouble();
        };
    int humidity =
        switch (condition) {
          case "Rainy", "Snowy" -> 70 + random.nextInt(31);
          default -> 30 + random.nextInt(51);
        };
    return new WeatherResponse(
        city,
        roundToOneDecimalPlace(temperature),
        condition,
        humidity,
        roundToOneDecimalPlace(20 * random.nextDouble()),
        WIND_DIRECTIONS[random.nextInt(WIND_DIRECTIONS.length)],
        LocalDateTime.now());
  }

  private double roundToOneDecimalPlace(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}
