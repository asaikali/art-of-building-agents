package com.example.jarvis.constraints;

import com.example.jarvis.constraints.deterministic.budget.BudgetPerPersonCheck;
import com.example.jarvis.constraints.deterministic.noise.NoiseLevelCheck;
import com.example.jarvis.constraints.deterministic.travel.TravelTimeCheck;
import com.example.jarvis.constraints.hybrid.dietary.DietarySuitabilityCheck;
import com.example.jarvis.constraints.llmjudge.suitability.MealSuitabilityCheck;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.jarvis.requirements.UserRequirements;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RestaurantCandidateCheckService {

  private final NoiseLevelCheck noiseLevelCheck;
  private final BudgetPerPersonCheck budgetPerPersonCheck;
  private final TravelTimeCheck travelTimeCheck;
  private final DietarySuitabilityCheck dietarySuitabilityCheck;
  private final MealSuitabilityCheck mealSuitabilityCheck;

  public RestaurantCandidateCheckService(
      NoiseLevelCheck noiseLevelCheck,
      BudgetPerPersonCheck budgetPerPersonCheck,
      TravelTimeCheck travelTimeCheck,
      DietarySuitabilityCheck dietarySuitabilityCheck,
      MealSuitabilityCheck mealSuitabilityCheck) {
    this.noiseLevelCheck = noiseLevelCheck;
    this.budgetPerPersonCheck = budgetPerPersonCheck;
    this.travelTimeCheck = travelTimeCheck;
    this.dietarySuitabilityCheck = dietarySuitabilityCheck;
    this.mealSuitabilityCheck = mealSuitabilityCheck;
  }

  public RestaurantCheckResult check(UserRequirements requirements, RestaurantCandidate candidate) {
    var meal = requirements.getMeal();
    var restaurantId = candidate.restaurantId();

    // Adapt attendee travel info for the travel time check — use the first attendee with travel
    // details
    var traveler = findFirstTraveler(requirements.getAttendees());

    return new RestaurantCheckResult(
        candidate,
        noiseLevelCheck.check(meal.getNoiseLevel(), restaurantId),
        budgetPerPersonCheck.check(meal.getBudgetPerPerson(), restaurantId),
        travelTimeCheck.check(
            traveler != null ? traveler.getOrigin() : null,
            traveler != null ? traveler.getTravelMode() : null,
            traveler != null ? traveler.getMaxTravelTimeMinutes() : null,
            restaurantId),
        dietarySuitabilityCheck.check(
            flattenDietaryConstraints(requirements.getAttendees()), restaurantId),
        mealSuitabilityCheck.check(meal, restaurantId));
  }

  private List<DietaryConstraint> flattenDietaryConstraints(List<Attendee> attendees) {
    return attendees.stream()
        .flatMap(attendee -> attendee.getDietaryConstraints().stream())
        .toList();
  }

  private Attendee findFirstTraveler(List<Attendee> attendees) {
    return attendees.stream()
        .filter(a -> a.getOrigin() != null || a.getTravelMode() != null)
        .findFirst()
        .orElse(null);
  }
}
