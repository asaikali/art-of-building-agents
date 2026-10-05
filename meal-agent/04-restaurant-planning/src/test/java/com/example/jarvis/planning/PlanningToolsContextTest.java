package com.example.jarvis.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.jarvis.constraints.RestaurantCandidate;
import com.example.jarvis.constraints.RestaurantCandidateCheckService;
import com.example.jarvis.requirements.UserRequirements;
import com.example.restaurant.Restaurant;
import com.example.restaurant.RestaurantService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;

class PlanningToolsContextTest {

  private final RestaurantService restaurants = mock(RestaurantService.class);
  private final RestaurantCandidateCheckService checks =
      mock(RestaurantCandidateCheckService.class);
  private final PlanningTools tools = new PlanningTools(null, restaurants, checks);

  @Test
  void toolCallbackKeepsRequirementsWithEachCallAndOutOfModelArguments() {
    var restaurant = mock(Restaurant.class);
    when(restaurant.id()).thenReturn("canoe");
    when(restaurant.name()).thenReturn("Canoe");
    when(restaurants.findById("canoe")).thenReturn(Optional.of(restaurant));

    var alex = new UserRequirements();
    alex.getMeal().setBudgetPerPerson(new BigDecimal("80"));
    var ben = new UserRequirements();
    ben.getMeal().setBudgetPerPerson(new BigDecimal("200"));
    var alexContext = new ToolContext(Map.of(PlanningTools.REQUIREMENTS, alex));
    var benContext = new ToolContext(Map.of(PlanningTools.REQUIREMENTS, ben));

    var callback =
        Arrays.stream(ToolCallbacks.from(tools))
            .filter(tool -> tool.getToolDefinition().name().equals("checkRestaurantCandidate"))
            .findFirst()
            .orElseThrow();
    assertThat(callback.getToolDefinition().inputSchema())
        .contains("restaurantId")
        .doesNotContain("toolContext", PlanningTools.REQUIREMENTS, "budgetPerPerson");

    // A shared tool instance must receive the right requirements on alternating calls.
    callback.call("{\"restaurantId\":\"canoe\"}", alexContext);
    callback.call("{\"restaurantId\":\"canoe\"}", benContext);
    callback.call("{\"restaurantId\":\"canoe\"}", alexContext);

    var candidate = new RestaurantCandidate("canoe", "Canoe");
    var calls = inOrder(checks);
    calls.verify(checks).check(same(alex), eq(candidate));
    calls.verify(checks).check(same(ben), eq(candidate));
    calls.verify(checks).check(same(alex), eq(candidate));
    calls.verifyNoMoreInteractions();
  }

  @Test
  void rejectsMissingRequirementsBeforeRunningChecks() {
    assertThatThrownBy(() -> tools.checkRestaurantCandidate("canoe", new ToolContext(Map.of())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("tool context");
    verifyNoInteractions(checks);
  }
}
