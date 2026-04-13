package com.example.jarvis.constraints.hybrid.dietary;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.DietaryConstraint;
import com.example.restaurant.MenuService;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

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

    return chatClient
        .prompt()
        .system(
            """
            You are judging whether a restaurant menu appears suitable for a group's dietary needs.

            Rules:
            - Focus on whether the menu offers substantive main courses that satisfy
              the dietary constraints. Appetizers, sides, and desserts alone are not enough.
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

                    <menuJson>
                    {menuJson}
                    </menuJson>
                    """)
                    .param("dietaryConstraints", JsonUtils.toJson(normalizedConstraints))
                    .param("menuJson", JsonUtils.toJson(menu.get())))
        .call()
        .entity(DietarySuitabilityResult.class);
  }
}
