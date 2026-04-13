package com.example.jarvis.requirements;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

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

  private static String normalizeText(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Attendee attendee = (Attendee) o;
    return Objects.equals(name, attendee.name)
        && Objects.equals(origin, attendee.origin)
        && Objects.equals(departureTime, attendee.departureTime)
        && travelMode == attendee.travelMode
        && Objects.equals(maxTravelTimeMinutes, attendee.maxTravelTimeMinutes)
        && Objects.equals(maxDistanceKm, attendee.maxDistanceKm)
        && Objects.equals(dietaryConstraints, attendee.dietaryConstraints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        name,
        origin,
        departureTime,
        travelMode,
        maxTravelTimeMinutes,
        maxDistanceKm,
        dietaryConstraints);
  }
}
