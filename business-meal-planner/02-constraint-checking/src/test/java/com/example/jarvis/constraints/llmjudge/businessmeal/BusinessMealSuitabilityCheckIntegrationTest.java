package com.example.jarvis.constraints.llmjudge.businessmeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jarvis.ConstraintCheckingApplication;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.MealType;
import com.example.jarvis.requirements.NoiseLevel;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = ConstraintCheckingApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
class BusinessMealSuitabilityCheckIntegrationTest {

  @Autowired private BusinessMealSuitabilityCheck check;

  @Test
  @DisplayName("Returns PASS or UNSURE for a polished client dinner venue")
  void returnsPassOrUnsureForPolishedClientDinnerVenue() {
    var meal = new Meal();
    meal.setMealType(MealType.DINNER);
    meal.setPurpose("client dinner");
    meal.setNoiseLevel(NoiseLevel.QUIET);
    meal.setBudgetPerPerson(new BigDecimal("120"));

    var result = check.check(meal, "canoe");

    assertThat(result.status())
        .isIn(BusinessMealSuitabilityStatus.PASS, BusinessMealSuitabilityStatus.UNSURE);
  }

  @Test
  @DisplayName("Returns FAIL or UNSURE for a loud venue for a client dinner")
  void returnsFailOrUnsureForLoudVenueForClientDinner() {
    var meal = new Meal();
    meal.setMealType(MealType.DINNER);
    meal.setPurpose("client dinner");
    meal.setNoiseLevel(NoiseLevel.QUIET);
    meal.setBudgetPerPerson(new BigDecimal("120"));

    var result = check.check(meal, "baro");

    assertThat(result.status())
        .isIn(BusinessMealSuitabilityStatus.FAIL, BusinessMealSuitabilityStatus.UNSURE);
  }

  @Test
  @DisplayName("Throws when restaurant id is blank")
  void throwsWhenRestaurantIdIsBlank() {
    assertThatThrownBy(() -> check.check(null, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantId must be provided");
  }
}
