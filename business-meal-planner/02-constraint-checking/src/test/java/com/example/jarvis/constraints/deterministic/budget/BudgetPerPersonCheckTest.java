package com.example.jarvis.constraints.deterministic.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.RestaurantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BudgetPerPersonCheckTest {

  private BudgetPerPersonCheck check;

  @BeforeEach
  void setUp() {
    var restaurantService = new RestaurantService(new ObjectMapper());
    check = new BudgetPerPersonCheck(restaurantService);
  }

  @Test
  @DisplayName("Returns PASS when no budget is provided")
  void returnsPassWhenNoBudgetIsProvided() {
    var result = check.check(null, "canoe");

    assertThat(result.status()).isEqualTo(BudgetPerPersonCheckStatus.PASS);
  }

  @Test
  @DisplayName("Returns PASS when the budget covers the restaurant's upper bound")
  void returnsPassWhenBudgetCoversUpperBound() {
    var result = check.check(new BigDecimal("120"), "canoe");

    assertThat(result.status()).isEqualTo(BudgetPerPersonCheckStatus.PASS);
  }

  @Test
  @DisplayName("Returns FAIL when the budget is below the restaurant's lower bound")
  void returnsFailWhenBudgetIsBelowLowerBound() {
    var result = check.check(new BigDecimal("60"), "canoe");

    assertThat(result.status()).isEqualTo(BudgetPerPersonCheckStatus.FAIL);
  }

  @Test
  @DisplayName("Returns MAYBE when the budget falls inside the restaurant's range")
  void returnsMaybeWhenBudgetFallsInsideRange() {
    var result = check.check(new BigDecimal("80"), "canoe");

    assertThat(result.status()).isEqualTo(BudgetPerPersonCheckStatus.MAYBE);
  }

  @Test
  @DisplayName("Throws when restaurant id is blank")
  void throwsWhenRestaurantIdIsBlank() {
    assertThatThrownBy(() -> check.check(new BigDecimal("80"), " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantId must be provided");
  }
}
