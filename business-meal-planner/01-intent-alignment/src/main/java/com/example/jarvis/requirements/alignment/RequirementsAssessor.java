package com.example.jarvis.requirements.alignment;

import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.MealType;
import com.example.jarvis.requirements.UserRequirements;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Assesses {@link UserRequirements} against hard and soft criteria. Used by the {@link
 * RequirementsAligner} after each extraction to determine what information is still needed and what
 * workflow status to assign.
 */
@Component
public class RequirementsAssessor {

  /**
   * Identifies the required fields that must be present before the alignment phase can move to
   * confirmation. These are the hard gates: date, time, and party size. Returns an empty list when
   * all required fields are present.
   */
  public List<String> findMissingRequiredFields(Meal meal) {
    List<String> missing = new ArrayList<>();
    if (meal == null || meal.getDate() == null) {
      missing.add("Date");
    }
    if (meal == null || meal.getTime() == null) {
      missing.add("Time");
    }
    if (meal == null || meal.getPartySize() == null || meal.getPartySize() <= 0) {
      missing.add("Party Size");
    }
    return missing;
  }

  /**
   * Determines the workflow status based on whether required fields are present and whether the
   * user has confirmed. Missing fields take priority — even if the user confirmed, the status stays
   * at clarification until all required fields are filled.
   */
  public RequirementStatus assessStatus(List<String> missingRequiredFields, boolean userConfirmed) {
    if (!missingRequiredFields.isEmpty()) {
      return RequirementStatus.WAITING_FOR_CLARIFICATION;
    }
    if (userConfirmed) {
      return RequirementStatus.REQUIREMENTS_CONFIRMED;
    }
    return RequirementStatus.WAITING_FOR_CONFIRMATION;
  }

  /**
   * Suggests optional follow-up questions that a good executive assistant would ask given the
   * current requirements. These are soft criteria — they don't block the workflow but help gather
   * useful details like dietary needs, budget, or noise preferences.
   */
  public List<String> suggestFollowUps(UserRequirements requirements) {
    List<String> suggestions = new ArrayList<>();
    Meal meal = requirements.getMeal();
    List<Attendee> attendees = requirements.getAttendees();

    if (meal.getPartySize() != null && meal.getPartySize() > 2 && noDietaryConstraints(attendees)) {
      suggestions.add("any dietary restrictions for the group");
    }

    if (meal.getBudgetPerPerson() == null && isHighStakesMeal(meal)) {
      suggestions.add("budget per person");
    }

    if (meal.getNoiseLevel() == null && meal.getPurpose() != null) {
      suggestions.add("preferred noise level");
    }

    if (meal.getPartySize() != null && meal.getPartySize() > 0 && attendees.isEmpty()) {
      suggestions.add(
          "attendee details such as names, where they're coming from, and dietary needs");
    }

    return suggestions;
  }

  private boolean noDietaryConstraints(List<Attendee> attendees) {
    return attendees.isEmpty()
        || attendees.stream().allMatch(a -> a.getDietaryConstraints().isEmpty());
  }

  private boolean isHighStakesMeal(Meal meal) {
    if (meal.getMealType() == MealType.DINNER) {
      return true;
    }
    String purpose = meal.getPurpose();
    return purpose != null && purpose.toLowerCase(Locale.ROOT).contains("client");
  }
}
