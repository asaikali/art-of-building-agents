package com.example.jarvis.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.jarvis.state.AgentState;
import com.example.jarvis.state.UserGoals;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntentAlignmentMarkdownRendererTest {

  private final IntentAlignmentMarkdownRenderer renderer = new IntentAlignmentMarkdownRenderer();

  @Test
  void rendersRequiredSectionsAndStatusLabel() {
    AgentState state = new AgentState();
    state.setUserGoals(
        new UserGoals(
            "Plan and book a business dinner.",
            LocalDate.of(2026, 4, 11),
            LocalTime.of(18, 0),
            4,
            List.of("Vegetarian guest", "Professional and quiet")));
    state.setAssumptions(List.of("Business appropriateness matters"));
    state.setMissingInformation(List.of());
    state.setStatus(RequirementStatus.WAITING_FOR_CONFIRMATION);

    String markdown = renderer.render(state);

    assertThat(markdown).contains("## Intent");
    assertThat(markdown).contains("## Minimum Requirements");
    assertThat(markdown).contains("Date: 2026-04-11");
    assertThat(markdown).contains("## Constraints");
    assertThat(markdown).contains("## Assumptions");
    assertThat(markdown).contains("## Missing Information");
    assertThat(markdown).contains("## Status");
    assertThat(markdown).contains("Waiting for confirmation");
    assertThat(markdown).contains("- Vegetarian guest");
  }

  @Test
  void rendersNoneForEmptyLists() {
    AgentState state = new AgentState();
    state.setUserGoals(new UserGoals("Clarify the request.", null, null, null, List.of()));
    state.setAssumptions(List.of());
    state.setMissingInformation(List.of());
    state.setStatus(RequirementStatus.WAITING_FOR_CLARIFICATION);

    String markdown = renderer.render(state);

    assertThat(markdown).contains("Date: Missing");
    assertThat(markdown).contains("## Constraints\n- None");
    assertThat(markdown).contains("## Assumptions\n- None");
    assertThat(markdown).contains("## Missing Information\n- None");
    assertThat(markdown).contains("Waiting for clarification");
  }
}
