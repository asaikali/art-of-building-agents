package com.example.jarvis.requirements;

import java.util.List;
import java.util.Objects;

/**
 * Captures the full set of requirements gathered from the user for this phase.
 *
 * <p>{@code UserRequirements} is the top-level planning input for Jarvis. It brings together the
 * shared meal information and the attendee-specific requirements so the rest of the system can work
 * with one clear representation of what the user wants.
 */
public class UserRequirements {

  private Meal meal = new Meal();
  private List<Attendee> attendees = List.of();

  public UserRequirements() {}

  public Meal getMeal() {
    return meal;
  }

  public void setMeal(Meal meal) {
    this.meal = meal == null ? new Meal() : meal;
  }

  public List<Attendee> getAttendees() {
    return attendees;
  }

  public void setAttendees(List<Attendee> attendees) {
    this.attendees = attendees == null ? List.of() : List.copyOf(attendees);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserRequirements that = (UserRequirements) o;
    return Objects.equals(meal, that.meal) && Objects.equals(attendees, that.attendees);
  }

  @Override
  public int hashCode() {
    return Objects.hash(meal, attendees);
  }
}
