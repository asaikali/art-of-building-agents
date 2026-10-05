package com.example.jarvis.constraints.hybrid.dietary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jarvis.DecisionSupportApplication;
import com.example.jarvis.requirements.DietaryConstraint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = DecisionSupportApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
class DietarySuitabilityCheckIntegrationTest {

  @Autowired private DietarySuitabilityCheck check;

  @Test
  @DisplayName("Returns PASS when there are no dietary constraints")
  void returnsPassWhenThereAreNoDietaryConstraints() {
    var result = check.check(List.of(DietaryConstraint.NONE), "canoe");

    assertThat(result.status()).isEqualTo(DietarySuitabilityStatus.PASS);
    assertThat(result.rationale()).isEqualTo("No dietary constraints need to be checked.");
  }

  @Test
  @DisplayName("Returns UNSURE when menu data is missing")
  void returnsUnsureWhenMenuDataIsMissing() {
    var result = check.check(List.of(DietaryConstraint.VEGETARIAN), "does-not-exist");

    assertThat(result.status()).isEqualTo(DietarySuitabilityStatus.UNSURE);
    assertThat(result.rationale()).isEqualTo("No menu data is available for this restaurant.");
  }

  @Test
  @DisplayName("Throws when restaurant id is blank")
  void throwsWhenRestaurantIdIsBlank() {
    assertThatThrownBy(() -> check.check(List.of(DietaryConstraint.VEGETARIAN), " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("restaurantId must be provided");
  }

  @Test
  @DisplayName("Planta Queen is a strong vegetarian fit")
  void plantaQueenIsAStrongVegetarianFit() {
    var result = check.check(List.of(DietaryConstraint.VEGETARIAN), "planta-queen");

    assertThat(result.status()).isEqualTo(DietarySuitabilityStatus.PASS);
  }

  @Test
  @DisplayName("Kintori Yakitori is not a strong vegetarian fit")
  void kintoriYakitoriIsNotAStrongVegetarianFit() {
    var result = check.check(List.of(DietaryConstraint.VEGETARIAN), "kintori-yakitori");

    assertThat(result.status())
        .isIn(DietarySuitabilityStatus.FAIL, DietarySuitabilityStatus.UNSURE);
  }
}
