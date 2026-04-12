package com.example.jarvis.requirements.alignment;

import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.MealType;
import com.example.jarvis.requirements.UserRequirements;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class RequirementsAssessor {

  public List<String> missingCriticalFields(Meal meal) {
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

  public RequirementStatus decideStatus(List<String> missingCriticalFields, boolean userConfirmed) {
    if (!missingCriticalFields.isEmpty()) {
      return RequirementStatus.WAITING_FOR_CLARIFICATION;
    }
    if (userConfirmed) {
      return RequirementStatus.REQUIREMENTS_CONFIRMED;
    }
    return RequirementStatus.WAITING_FOR_CONFIRMATION;
  }

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
