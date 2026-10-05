package com.example.jarvis.preferences;

import com.example.restaurant.Restaurant;
import com.example.restaurant.RestaurantService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class UserPreferenceTools {
  private final UserPreferenceStore store;
  private final RestaurantService restaurants;

  public UserPreferenceTools(UserPreferenceStore store, RestaurantService restaurants) {
    this.store = store;
    this.restaurants = restaurants;
  }

  @Tool(
      description =
          "Remember a favourite restaurant for future meals. Use only when the user explicitly asks to remember it. Use the restaurant ID or name.")
  public String rememberFavourite(
      String restaurantId,
      @ToolParam(description = "Occasion such as client dinners, or empty for any occasion")
          String occasion,
      ToolContext context) {
    var restaurant = resolve(restaurantId);
    store.remember(
        userId(context),
        new UserPreferenceStore.Favourite(restaurant.id(), restaurant.name(), occasion));
    return "Remembered " + restaurant.name() + " as a favourite.";
  }

  @Tool(
      description =
          "Forget a favourite restaurant. Use only when the user explicitly asks to forget it. Use the restaurant ID or name.")
  public String forgetFavourite(String restaurantId, ToolContext context) {
    var restaurant = resolve(restaurantId);
    boolean removed = store.forget(userId(context), restaurant.id());
    return removed
        ? "Forgot " + restaurant.name() + " as a favourite."
        : "That restaurant was not a saved favourite.";
  }

  private Restaurant resolve(String id) {
    var exact = restaurants.findById(id);
    if (exact.isPresent()) return exact.get();
    var matches =
        restaurants.findAll().stream()
            .filter(
                r ->
                    r.name()
                        .toLowerCase(java.util.Locale.ROOT)
                        .contains(id.toLowerCase(java.util.Locale.ROOT)))
            .toList();
    if (!id.isBlank() && matches.size() == 1) return matches.getFirst();
    throw new IllegalArgumentException("Unknown or ambiguous restaurant: " + id);
  }

  private String userId(ToolContext context) {
    Object identity = context.getContext().get(UserPreferenceAdvisor.USER_ID);
    if (!(identity instanceof String id) || id.isBlank())
      throw new IllegalArgumentException("Session user ID is required");
    return id;
  }
}
