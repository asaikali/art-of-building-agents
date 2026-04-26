package com.example.jarvis.constraints.hybrid.dietary;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.restaurant.MenuSection;
import com.example.restaurant.MenuService;
import com.example.restaurant.RestaurantMenu;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Hybrid check that judges whether a restaurant's main courses appear suitable for the party's
 * dietary needs.
 *
 * <p>The Java half does deterministic preparation:
 *
 * <ul>
 *   <li>de-duplicates dietary constraints and ignores {@link
 *       com.example.jarvis.requirements.DietaryConstraint#NONE}
 *   <li>loads the menu and filters out non-main sections (appetizers, desserts, sides, drinks,
 *       sauces, …) so the model focuses on what people will actually order as a main
 * </ul>
 *
 * <p>The LLM half then judges restaurant-level suitability from the filtered evidence and returns
 * PASS, FAIL, or UNSURE.
 *
 * <p>Two short-circuits avoid the model call entirely:
 *
 * <ul>
 *   <li>If the normalized constraint list is empty, return {@link DietarySuitabilityStatus#PASS} —
 *       there is nothing to check.
 *   <li>If no menu data exists for the restaurant, return {@link DietarySuitabilityStatus#UNSURE} —
 *       there is nothing to judge.
 * </ul>
 */
@Component
public class DietarySuitabilityCheck {

  private final MenuService menuService;
  private final ChatClient chatClient;

  public DietarySuitabilityCheck(MenuService menuService, ChatClient.Builder chatClientBuilder) {
    this.menuService = menuService;
    this.chatClient = chatClientBuilder.build();
  }

  public DietarySuitabilityResult check(
      List<DietaryConstraint> dietaryConstraints, String restaurantId) {

    if (restaurantId == null || restaurantId.isBlank()) {
      throw new IllegalArgumentException("restaurantId must be provided");
    }

    var normalizedConstraints =
        dietaryConstraints == null
            ? List.<DietaryConstraint>of()
            : dietaryConstraints.stream()
                .filter(constraint -> constraint != null && constraint != DietaryConstraint.NONE)
                .distinct()
                .toList();

    if (normalizedConstraints.isEmpty()) {
      return new DietarySuitabilityResult(
          DietarySuitabilityStatus.PASS, "No dietary constraints need to be checked.");
    }

    var menu = menuService.findById(restaurantId);
    if (menu.isEmpty()) {
      return new DietarySuitabilityResult(
          DietarySuitabilityStatus.UNSURE, "No menu data is available for this restaurant.");
    }

    var mainCourseSections = filterToMainCourses(menu.get());
    if (mainCourseSections.isEmpty()) {
      return new DietarySuitabilityResult(
          DietarySuitabilityStatus.UNSURE,
          "No main course sections found in the menu to evaluate.");
    }

    return chatClient
        .prompt()
        .system(
            """
            You are judging whether a restaurant's main courses appear suitable for a
            group's dietary needs.

            Rules:
            - Judge the restaurant overall, not each item individually.
            - Use only the dietary constraints and menu evidence provided.
            - Do not invent menu items or dietary accommodations.
            - Return PASS, FAIL, or UNSURE.
            - Be conservative when evidence is weak or ambiguous.
            """)
        .user(
            u ->
                u.text(
                        """
                    <dietaryConstraints>
                    {dietaryConstraints}
                    </dietaryConstraints>

                    <mainCourseMenu>
                    {mainCourseMenu}
                    </mainCourseMenu>
                    """)
                    .param("dietaryConstraints", JsonUtils.toJson(normalizedConstraints))
                    .param("mainCourseMenu", JsonUtils.toJson(mainCourseSections)))
        .call()
        .entity(DietarySuitabilityResult.class);
  }

  private static final Set<String> NON_MAIN_KEYWORDS =
      Set.of(
          "appetizer",
          "starter",
          "dessert",
          "side",
          "snack",
          "drink",
          "beverage",
          "cocktail",
          "wine",
          "beer",
          "sauce",
          "add-on",
          "enhancement",
          "spread");

  private List<MenuSection> filterToMainCourses(RestaurantMenu menu) {
    return menu.menuSections().stream()
        .filter(section -> !isNonMainSection(section.name()))
        .toList();
  }

  private boolean isNonMainSection(String sectionName) {
    var lower = sectionName.toLowerCase(Locale.ROOT);
    return NON_MAIN_KEYWORDS.stream().anyMatch(lower::contains);
  }
}
