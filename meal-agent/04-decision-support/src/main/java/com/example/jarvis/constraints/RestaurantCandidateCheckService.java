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

/**
 * Runs every Phase 2 constraint check against one {@link RestaurantCandidate} and returns a
 * strongly typed {@link RestaurantCheckResult} aggregating the verdicts.
 *
 * <p>This service is an orchestration and adapter layer. It invokes the underlying checks and
 * adapts the higher-level {@link UserRequirements} into the narrower input shapes those checks
 * expect — for example, flattening per-attendee dietary constraints into a single {@code
 * List<DietaryConstraint>} for {@link DietarySuitabilityCheck}, or picking the first attendee with
 * travel info for {@link TravelTimeCheck}.
 *
 * <p>The flattening and traveler-selection are the real adapter responsibility of this layer: the
 * lower-level checks deliberately do not depend on {@code UserRequirements}, which keeps them
 * narrow and reusable.
 *
 * <p>This service does not score the candidate, classify any check as hard vs soft, decide whether
 * the candidate should be rejected, or rank candidates against each other. Those interpretations
 * belong to the planning layer in later phases.
 */
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
