package com.example.jarvis.requirements;

import java.util.List;

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

  public String toMarkdown() {
    return """
        %s

        ## Attendees
        %s
        """
        .formatted(meal.toMarkdown(), renderAttendees(attendees));
  }

  public boolean isEmpty() {
    return meal.getDate() == null
        && meal.getTime() == null
        && meal.getPartySize() == null
        && meal.getMealType() == null
        && meal.getPurpose() == null
        && meal.getBudgetPerPerson() == null
        && meal.getNoiseLevel() == null
        && meal.getAdditionalRequirements().isEmpty()
        && meal.getCuisinePreferences().isEmpty()
        && attendees.isEmpty();
  }

  private static String renderAttendees(List<Attendee> attendees) {
    if (attendees.isEmpty()) {
      return "- None";
    }
    return attendees.stream()
        .map(Attendee::toMarkdown)
        .collect(java.util.stream.Collectors.joining("\n"));
  }
}
