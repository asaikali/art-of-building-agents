package com.example.jarvis.requirements;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Captures the shared information about the meal being planned.
 *
 * <p>{@code Meal} is the shared description of the meal itself: when it happens, what kind of meal
 * it is, and what the overall experience should support. It stays focused on captured input so
 * later phases can build planning and decision logic on top of a stable representation of what the
 * user wants.
 */
public class Meal {

  private LocalDate date;
  private LocalTime time;
  private Integer partySize;
  private MealType mealType;
  private String purpose;
  private BigDecimal budgetPerPerson;
  private NoiseLevel noiseLevel;
  private List<String> additionalRequirements = List.of();
  private List<String> cuisinePreferences = List.of();

  public Meal() {}

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalTime getTime() {
    return time;
  }

  public void setTime(LocalTime time) {
    this.time = time;
  }

  public Integer getPartySize() {
    return partySize;
  }

  public void setPartySize(Integer partySize) {
    this.partySize = partySize;
  }

  public MealType getMealType() {
    return mealType;
  }

  public void setMealType(MealType mealType) {
    this.mealType = mealType;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = normalizeText(purpose);
  }

  public BigDecimal getBudgetPerPerson() {
    return budgetPerPerson;
  }

  public void setBudgetPerPerson(BigDecimal budgetPerPerson) {
    this.budgetPerPerson = budgetPerPerson;
  }

  public NoiseLevel getNoiseLevel() {
    return noiseLevel;
  }

  public void setNoiseLevel(NoiseLevel noiseLevel) {
    this.noiseLevel = noiseLevel;
  }

  public List<String> getAdditionalRequirements() {
    return additionalRequirements;
  }

  public void setAdditionalRequirements(List<String> additionalRequirements) {
    this.additionalRequirements = sanitize(additionalRequirements);
  }

  public List<String> getCuisinePreferences() {
    return cuisinePreferences;
  }

  public void setCuisinePreferences(List<String> cuisinePreferences) {
    this.cuisinePreferences = sanitize(cuisinePreferences);
  }

  private static String normalizeText(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static List<String> sanitize(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .toList();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Meal meal = (Meal) o;
    return Objects.equals(date, meal.date)
        && Objects.equals(time, meal.time)
        && Objects.equals(partySize, meal.partySize)
        && mealType == meal.mealType
        && Objects.equals(purpose, meal.purpose)
        && Objects.equals(budgetPerPerson, meal.budgetPerPerson)
        && noiseLevel == meal.noiseLevel
        && Objects.equals(additionalRequirements, meal.additionalRequirements)
        && Objects.equals(cuisinePreferences, meal.cuisinePreferences);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        date,
        time,
        partySize,
        mealType,
        purpose,
        budgetPerPerson,
        noiseLevel,
        additionalRequirements,
        cuisinePreferences);
  }
}
