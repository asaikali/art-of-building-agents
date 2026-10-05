package com.example.jarvis.preferences;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.restaurant.Restaurant;
import com.example.restaurant.RestaurantService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;

class UserPreferenceMemoryTest {
  @Test
  void explicitMemoryToolsUseApplicationIdentityAndKeepUsersSeparate() {
    var store = new UserPreferenceStore();
    var restaurants = mock(RestaurantService.class);
    var canoe = mock(Restaurant.class);
    when(canoe.id()).thenReturn("canoe");
    when(canoe.name()).thenReturn("Canoe");
    when(restaurants.findById("canoe")).thenReturn(java.util.Optional.of(canoe));
    when(restaurants.findAll()).thenReturn(java.util.List.of(canoe));
    var tools = new UserPreferenceTools(store, restaurants);
    var alex = new ToolContext(Map.of(UserPreferenceAdvisor.USER_ID, "alex"));
    var ben = new ToolContext(Map.of(UserPreferenceAdvisor.USER_ID, "ben"));

    tools.rememberFavourite("Canoe", "client dinners", alex);
    assertEquals(1, store.favourites("alex").size());
    assertTrue(store.favourites("ben").isEmpty());
    tools.forgetFavourite("canoe", ben);
    assertEquals(1, store.favourites("alex").size());

    var advisor = new UserPreferenceAdvisor(store);
    var chain = mock(CallAdvisorChain.class);
    var request =
        new ChatClientRequest(
            new Prompt("Find dinner"), Map.of(UserPreferenceAdvisor.USER_ID, "alex"));
    advisor.adviseCall(request, chain);
    var captured = org.mockito.ArgumentCaptor.forClass(ChatClientRequest.class);
    verify(chain).nextCall(captured.capture());
    String enriched = captured.getValue().prompt().getUserMessage().getText();
    assertTrue(enriched.contains("Canoe"));
    assertTrue(enriched.contains("Find dinner"));
    assertTrue(enriched.contains("Current user instructions override"));

    // User identity is not a model-supplied tool argument.
    for (var callback : ToolCallbacks.from(tools)) {
      assertFalse(callback.getToolDefinition().inputSchema().contains("meal.userId"));
      assertFalse(callback.getToolDefinition().inputSchema().contains("context"));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> advisor.adviseCall(new ChatClientRequest(new Prompt("Dinner"), Map.of()), chain));
    assertThrows(
        IllegalArgumentException.class, () -> tools.rememberFavourite("unknown", "", alex));
    tools.forgetFavourite("canoe", alex);
    assertTrue(store.favourites("alex").isEmpty());
  }
}
