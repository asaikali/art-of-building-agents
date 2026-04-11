package com.example.jarvis.requirements;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/**
 * Captures requirements that belong to one specific person attending the meal.
 *
 * <p>Use {@code Attendee} for anything that can differ from person to person, such as origin,
 * travel preferences, or dietary needs. Keeping those details here prevents event-level
 * requirements from turning into a mixed bag of unrelated constraints.
 */
public class Attendee {

  private String name;
  private String origin;
  private LocalTime departureTime;
  private TravelMode travelMode;
  private Integer maxTravelTimeMinutes;
  private Double maxDistanceKm;
  private List<DietaryConstraint> dietaryConstraints = List.of();

  public Attendee() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = normalizeText(name);
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = normalizeText(origin);
  }

  public LocalTime getDepartureTime() {
    return departureTime;
  }

  public void setDepartureTime(LocalTime departureTime) {
    this.departureTime = departureTime;
  }

  public TravelMode getTravelMode() {
    return travelMode;
  }

  public void setTravelMode(TravelMode travelMode) {
    this.travelMode = travelMode;
  }

  public Integer getMaxTravelTimeMinutes() {
    return maxTravelTimeMinutes;
  }

  public void setMaxTravelTimeMinutes(Integer maxTravelTimeMinutes) {
    this.maxTravelTimeMinutes = maxTravelTimeMinutes;
  }

  public Double getMaxDistanceKm() {
    return maxDistanceKm;
  }

  public void setMaxDistanceKm(Double maxDistanceKm) {
    this.maxDistanceKm = maxDistanceKm;
  }

  public List<DietaryConstraint> getDietaryConstraints() {
    return dietaryConstraints;
  }

  public void setDietaryConstraints(List<DietaryConstraint> dietaryConstraints) {
    this.dietaryConstraints =
        dietaryConstraints == null ? List.of() : List.copyOf(dietaryConstraints);
  }

  public String toMarkdown() {
    String dietarySummary =
        dietaryConstraints.isEmpty()
            ? "none"
            : dietaryConstraints.stream()
                .map(Enum::name)
                .map(value -> value.toLowerCase(Locale.ROOT).replace('_', ' '))
                .collect(java.util.stream.Collectors.joining(", "));

    return "- Name: %s | Origin: %s | Departure Time: %s | Travel Mode: %s | Max Travel Time: %s | Max Distance: %s | Dietary Constraints: %s"
        .formatted(
            renderValue(name),
            renderValue(origin),
            renderValue(departureTime),
            renderEnum(travelMode),
            renderValue(maxTravelTimeMinutes),
            renderValue(maxDistanceKm),
            dietarySummary);
  }

  private static String normalizeText(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String renderValue(Object value) {
    return value == null ? "Missing" : value.toString();
  }

  private static String renderEnum(Enum<?> value) {
    if (value == null) {
      return "Missing";
    }
    return value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
  }
}
