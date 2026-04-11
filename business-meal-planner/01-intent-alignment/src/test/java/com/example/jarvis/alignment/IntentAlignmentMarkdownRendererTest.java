package com.example.jarvis.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntentAlignmentMarkdownRendererTest {

  private final IntentAlignmentMarkdownRenderer renderer = new IntentAlignmentMarkdownRenderer();

  @Test
  void rendersRequiredSectionsAndStatusLabel() {
    BusinessMealRequirements requirements =
        new BusinessMealRequirements(
            "Plan and book a business dinner.",
            java.util.List.of("Dinner tonight at 6:00 PM", "4 people"),
            java.util.List.of("Venue should be suitable for business conversation"),
            java.util.List.of("Budget"),
            java.util.List.of("Business appropriateness matters"),
            RequirementStatus.WAITING_FOR_CONFIRMATION);

    String markdown = renderer.render(requirements);

    assertThat(markdown).contains("## Intent");
    assertThat(markdown).contains("## Explicit Constraints");
    assertThat(markdown).contains("## Inferred Constraints");
    assertThat(markdown).contains("## Missing Information");
    assertThat(markdown).contains("## Assumptions");
    assertThat(markdown).contains("## Status");
    assertThat(markdown).contains("Waiting for confirmation");
    assertThat(markdown).contains("- Dinner tonight at 6:00 PM");
  }

  @Test
  void rendersNoneForEmptyLists() {
    BusinessMealRequirements requirements =
        new BusinessMealRequirements(
            "Clarify the request.",
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            RequirementStatus.WAITING_FOR_CLARIFICATION);

    String markdown = renderer.render(requirements);

    assertThat(markdown).contains("## Explicit Constraints\n- None");
    assertThat(markdown).contains("## Inferred Constraints\n- None");
    assertThat(markdown).contains("## Missing Information\n- None");
    assertThat(markdown).contains("## Assumptions\n- None");
    assertThat(markdown).contains("Waiting for clarification");
  }
}
