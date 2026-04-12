package com.example.jarvis.constraints.llmjudge.businessmeal;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.Meal;
import com.example.restaurant.RestaurantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class BusinessMealSuitabilityCheck {

  private final RestaurantService restaurantService;
  private final ChatClient chatClient;

  public BusinessMealSuitabilityCheck(
      RestaurantService restaurantService, ChatClient.Builder chatClientBuilder) {
    this.restaurantService = restaurantService;
    this.chatClient = chatClientBuilder.build();
  }

  public BusinessMealSuitabilityResult check(Meal meal, String restaurantId) {
    if (restaurantId == null || restaurantId.isBlank()) {
      throw new IllegalArgumentException("restaurantId must be provided");
    }

    var restaurant =
        restaurantService
            .findById(restaurantId)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown restaurant id: " + restaurantId));

    var mealContext =
        meal == null
            ? null
            : new MealContext(
                meal.getMealType(),
                meal.getPurpose(),
                meal.getNoiseLevel(),
                meal.getBudgetPerPerson());

    var restaurantContext =
        new RestaurantContext(
            restaurant.name(),
            restaurant.neighborhood(),
            restaurant.noiseLevel(),
            restaurant.priceRangePerPerson(),
            restaurant.description());

    return chatClient
        .prompt()
        .system(
            """
            You are judging whether a restaurant appears suitable for the user's stated
            meal purpose and context.

            Rules:
            - Use only the meal context and restaurant details provided.
            - Focus on whether the venue fits the meal purpose, likely atmosphere,
              conversation suitability, and business appropriateness.
            - Do not invent facts beyond the provided restaurant metadata.
            - Be conservative when the evidence is weak.
            - Return PASS, FAIL, or UNSURE.
            """)
        .user(
            u ->
                u.text(
                        """
                    <mealContext>
                    {mealContext}
                    </mealContext>

                    <restaurantContext>
                    {restaurantContext}
                    </restaurantContext>
                    """)
                    .param("mealContext", JsonUtils.toJson(mealContext))
                    .param("restaurantContext", JsonUtils.toJson(restaurantContext)))
        .call()
        .entity(BusinessMealSuitabilityResult.class);
  }

  private record MealContext(
      Object mealType, String purpose, Object requestedNoiseLevel, Object budgetPerPerson) {}

  private record RestaurantContext(
      String name,
      String neighborhood,
      String noiseLevel,
      Object priceRangePerPerson,
      String description) {}
}
