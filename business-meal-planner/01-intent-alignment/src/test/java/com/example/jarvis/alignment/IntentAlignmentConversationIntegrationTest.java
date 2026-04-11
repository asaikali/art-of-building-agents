package com.example.jarvis.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.session.SessionId;
import com.example.jarvis.IntentAlignmentApplication;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Real-model integration test for transcript-style alignment scenarios.
 *
 * <p>Opt-in only:
 *
 * <pre>
 * ./mvnw -pl business-meal-planner/01-intent-alignment \
 *   -Djarvis.openai.integration=true \
 *   -Dtest=IntentAlignmentConversationIntegrationTest test
 * </pre>
 */
@SpringBootTest(
    classes = IntentAlignmentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfSystemProperty(named = "jarvis.openai.integration", matches = "true")
class IntentAlignmentConversationIntegrationTest {

  @Autowired private IntentAlignmentConversationService service;

  @Autowired private IntentAlignmentMarkdownRenderer renderer;

  @Test
  void supportsLongBackAndForthTranscriptAgainstConfiguredModel() {
    TranscriptScenario scenario = scenario(10_001);

    scenario
        .user(
            """
            I have a client dinner tomorrow at 7 pm for 4 people. One guest is vegetarian.
            I want somewhere professional and quiet enough to talk.
            """)
        .hasAction(IntentAlignmentAction.PLAN_GENERATED)
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .requirementsMention("dinner", "vegetarian", "professional")
        .assistantMentionsAnyOf("confirm", "correct")
        .markdownHasRequiredSections();

    scenario
        .user("Don't book yet. Keep it under 120 CAD per person and near Union Station.")
        .hasAction(IntentAlignmentAction.PLAN_UPDATED)
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .requirementsMention("120")
        .requirementsMentionAnyOf("union", "station")
        .assistantMentionsAnyOf("confirm", "correct");

    scenario
        .user("not sure")
        .hasAction(IntentAlignmentAction.CLARIFICATION_REQUESTED)
        .hasStatus(RequirementStatus.WAITING_FOR_CLARIFICATION)
        .assistantLooksLikeQuestion();

    scenario
        .user("Let's optimize for low noise and easy conversation with a client.")
        .hasAction(IntentAlignmentAction.PLAN_UPDATED)
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .requirementsMentionAnyOf("quiet", "low noise", "conversation", "client")
        .assistantMentionsAnyOf("confirm", "correct");

    scenario
        .user("yes")
        .hasAction(IntentAlignmentAction.REQUIREMENTS_CONFIRMED)
        .hasStatus(RequirementStatus.REQUIREMENTS_CONFIRMED)
        .assistantMentionsAnyOf("confirmed", "requirements are confirmed");
  }

  @Test
  void supportsClarifyThenConfirmTranscriptAgainstConfiguredModel() {
    TranscriptScenario scenario = scenario(10_002);

    scenario
        .user("Help me plan a business meal.")
        .hasAction(IntentAlignmentAction.CLARIFICATION_REQUESTED)
        .hasStatus(RequirementStatus.WAITING_FOR_CLARIFICATION)
        .assistantLooksLikeQuestion()
        .markdownHasRequiredSections();

    scenario
        .user(
            """
            It's an internal team lunch next Tuesday for 6 people, one gluten-free,
            and I only want recommendations.
            """)
        .hasAction(IntentAlignmentAction.PLAN_UPDATED)
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .requirementsMention("lunch", "6", "gluten")
        .turnMentionsAnyOf("recommend", "recommendations", "do not book", "don't book")
        .assistantMentionsAnyOf("confirm", "correct");

    scenario
        .user("exactly")
        .hasAction(IntentAlignmentAction.REQUIREMENTS_CONFIRMED)
        .hasStatus(RequirementStatus.REQUIREMENTS_CONFIRMED)
        .assistantMentionsAnyOf("confirmed", "requirements are confirmed");
  }

  private TranscriptScenario scenario(int sessionIdValue) {
    return new TranscriptScenario(new SessionId(sessionIdValue));
  }

  private final class TranscriptScenario {

    private final SessionId sessionId;

    private TranscriptScenario(SessionId sessionId) {
      this.sessionId = sessionId;
    }

    private TranscriptTurn user(String text) {
      return new TranscriptTurn(service.handleTurn(sessionId, text));
    }
  }

  private final class TranscriptTurn {

    private final IntentAlignmentTurnResult result;

    private TranscriptTurn(IntentAlignmentTurnResult result) {
      this.result = result;
    }

    private TranscriptTurn hasAction(IntentAlignmentAction expected) {
      assertThat(result.action()).isEqualTo(expected);
      return this;
    }

    private TranscriptTurn hasStatus(RequirementStatus expected) {
      assertThat(result.requirements().status()).isEqualTo(expected);
      return this;
    }

    private TranscriptTurn requirementsMention(String... terms) {
      String haystack = flattenedRequirements();
      for (String term : terms) {
        assertThat(haystack).contains(term.toLowerCase(Locale.ROOT));
      }
      return this;
    }

    private TranscriptTurn requirementsMentionAnyOf(String... terms) {
      String haystack = flattenedRequirements();
      assertThat(
              Arrays.stream(terms)
                  .map(term -> term.toLowerCase(Locale.ROOT))
                  .anyMatch(haystack::contains))
          .isTrue();
      return this;
    }

    private TranscriptTurn assistantMentionsAnyOf(String... terms) {
      String haystack = result.assistantReply().toLowerCase(Locale.ROOT);
      assertThat(
              Arrays.stream(terms)
                  .map(term -> term.toLowerCase(Locale.ROOT))
                  .anyMatch(haystack::contains))
          .isTrue();
      return this;
    }

    private TranscriptTurn turnMentionsAnyOf(String... terms) {
      String haystack =
          (flattenedRequirements() + "\n" + result.assistantReply()).toLowerCase(Locale.ROOT);
      assertThat(
              Arrays.stream(terms)
                  .map(term -> term.toLowerCase(Locale.ROOT))
                  .anyMatch(haystack::contains))
          .isTrue();
      return this;
    }

    private TranscriptTurn assistantLooksLikeQuestion() {
      String reply = result.assistantReply();
      assertThat(reply).contains("?");
      return this;
    }

    private TranscriptTurn markdownHasRequiredSections() {
      String markdown = renderer.render(result.requirements());
      assertThat(markdown).contains("## Intent");
      assertThat(markdown).contains("## Explicit Constraints");
      assertThat(markdown).contains("## Inferred Constraints");
      assertThat(markdown).contains("## Missing Information");
      assertThat(markdown).contains("## Assumptions");
      assertThat(markdown).contains("## Status");
      return this;
    }

    private String flattenedRequirements() {
      BusinessMealRequirements requirements = result.requirements();
      return String.join(
              "\n",
              requirements.intent(),
              String.join("\n", requirements.explicitConstraints()),
              String.join("\n", requirements.inferredConstraints()),
              String.join("\n", requirements.missingInformation()),
              String.join("\n", requirements.assumptions()))
          .toLowerCase(Locale.ROOT);
    }
  }
}
