package com.example.jarvis.state;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class UserGoals {

  private String intent;
  private LocalDate date;
  private LocalTime time;
  private Integer partySize;
  private List<String> constraints = List.of();

  public UserGoals() {}

  public UserGoals(
      String intent, LocalDate date, LocalTime time, Integer partySize, List<String> constraints) {
    this.intent = normalizeIntent(intent);
    this.date = date;
    this.time = time;
    this.partySize = partySize;
    this.constraints = sanitize(constraints);
  }

  public static UserGoals fromValues(
      String intent, LocalDate date, LocalTime time, Integer partySize, List<String> constraints) {
    return new UserGoals(intent, date, time, partySize, constraints);
  }

  public String getIntent() {
    return intent;
  }

  public void setIntent(String intent) {
    this.intent = normalizeIntent(intent);
  }

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

  public List<String> getConstraints() {
    return constraints;
  }

  public void setConstraints(List<String> constraints) {
    this.constraints = sanitize(constraints);
  }

  private static String normalizeIntent(String value) {
    if (value == null || value.isBlank()) {
      return "Clarify the business meal request.";
    }
    return value.trim();
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
}
