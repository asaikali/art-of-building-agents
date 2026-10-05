package com.example.jarvis.constraints.deterministic.budget;

import com.example.restaurant.RestaurantService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Deterministic check comparing the user's per-person budget to the restaurant's published price
 * range.
 *
 * <p>Three outcomes are possible:
 *
 * <ul>
 *   <li>{@link BudgetPerPersonCheckStatus#PASS} — budget is at or above the restaurant's upper
 *       bound (clearly affordable)
 *   <li>{@link BudgetPerPersonCheckStatus#FAIL} — budget is below the restaurant's lower bound
 *       (clearly out of reach)
 *   <li>{@link BudgetPerPersonCheckStatus#MAYBE} — budget falls inside the published range (could
 *       go either way once specific dishes are picked)
 * </ul>
 *
 * <p>The MAYBE status exists because the fake pricing data is coarse: a budget that lands inside
 * the published range is informative but not conclusive. If no budget is provided the check returns
 * PASS, since there is no constraint to violate.
 */
@Component
public class BudgetPerPersonCheck {

  private final RestaurantService restaurantService;

  public BudgetPerPersonCheck(RestaurantService restaurantService) {
    this.restaurantService = restaurantService;
  }

  public BudgetPerPersonCheckResult check(
      BigDecimal requestedBudgetPerPerson, String restaurantId) {
    if (restaurantId == null || restaurantId.isBlank()) {
      throw new IllegalArgumentException("restaurantId must be provided");
    }

    if (requestedBudgetPerPerson == null) {
      return new BudgetPerPersonCheckResult(
          BudgetPerPersonCheckStatus.PASS, "No per-person budget was provided.");
    }

    var restaurant =
        restaurantService
            .findById(restaurantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown restaurant id: " + restaurantId));

    var priceRange = restaurant.priceRangePerPerson();
    if (priceRange == null) {
      throw new IllegalStateException("Restaurant price range must be present");
    }
    if (priceRange.min() == null || priceRange.max() == null) {
      throw new IllegalStateException(
          "Restaurant price range min/max must be present for: " + restaurantId);
    }

    var min = BigDecimal.valueOf(priceRange.min());
    var max = BigDecimal.valueOf(priceRange.max());

    if (requestedBudgetPerPerson.compareTo(max) >= 0) {
      return new BudgetPerPersonCheckResult(
          BudgetPerPersonCheckStatus.PASS,
          "Budget %s covers the restaurant range %s-%s %s."
              .formatted(
                  requestedBudgetPerPerson.toPlainString(),
                  min.toPlainString(),
                  max.toPlainString(),
                  priceRange.currency()));
    }

    if (requestedBudgetPerPerson.compareTo(min) < 0) {
      return new BudgetPerPersonCheckResult(
          BudgetPerPersonCheckStatus.FAIL,
          "Budget %s is below the restaurant range %s-%s %s."
              .formatted(
                  requestedBudgetPerPerson.toPlainString(),
                  min.toPlainString(),
                  max.toPlainString(),
                  priceRange.currency()));
    }

    return new BudgetPerPersonCheckResult(
        BudgetPerPersonCheckStatus.MAYBE,
        "Budget %s falls inside the restaurant range %s-%s %s."
            .formatted(
                requestedBudgetPerPerson.toPlainString(),
                min.toPlainString(),
                max.toPlainString(),
                priceRange.currency()));
  }
}
