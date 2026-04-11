package com.example.jarvis.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.session.SessionId;
import com.example.jarvis.IntentAlignmentApplication;
import com.example.jarvis.state.AgentState;
import com.example.jarvis.state.UserGoals;
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
        .hasEventName("plan-generated")
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .goalsMention("dinner", "vegetarian", "professional")
        .assistantMentionsAnyOf("confirm", "correct")
        .markdownHasRequiredSections();

    scenario
        .user("Don't book yet. I'm leaving from Union Station. Keep it under 120 CAD per person.")
        .hasEventName("plan-updated")
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .goalsMention("120")
        .goalsMentionAnyOf("union", "station")
        .assistantMentionsAnyOf("confirm", "correct");

    scenario
        .user("not sure")
        .hasEventName("clarification-requested")
        .hasStatus(RequirementStatus.WAITING_FOR_CLARIFICATION)
        .assistantLooksLikeQuestion();

    scenario
        .user("Let's optimize for low noise and easy conversation with a client.")
        .hasEventName("plan-updated")
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .goalsMentionAnyOf("quiet", "low noise", "conversation", "client")
        .assistantMentionsAnyOf("confirm", "correct");

    scenario
        .user("yes")
        .hasEventName("requirements-confirmed")
        .hasStatus(RequirementStatus.REQUIREMENTS_CONFIRMED)
        .assistantMentionsAnyOf("confirmed", "requirements are confirmed");
  }

  @Test
  void supportsClarifyThenConfirmTranscriptAgainstConfiguredModel() {
    TranscriptScenario scenario = scenario(10_002);

    scenario
        .user("Help me plan a business meal.")
        .hasEventName("clarification-requested")
        .hasStatus(RequirementStatus.WAITING_FOR_CLARIFICATION)
        .assistantMentionsAnyOf("date", "time", "party size")
        .markdownHasRequiredSections();

    scenario
        .user(
            """
            It's an internal team lunch next Tuesday for 6 people, one gluten-free,
            and I only want recommendations.
            """)
        .hasEventName("plan-updated")
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .goalsMention("lunch", "6", "gluten")
        .turnMentionsAnyOf("recommend", "recommendations")
        .assistantMentionsAnyOf("confirm", "correct");

    scenario
        .user("exactly")
        .hasEventName("requirements-confirmed")
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

    private final IntentAlignmentConversationService.TurnResult result;

    private TranscriptTurn(IntentAlignmentConversationService.TurnResult result) {
      this.result = result;
    }

    private TranscriptTurn hasEventName(String expected) {
      assertThat(result.eventName()).isEqualTo(expected);
      return this;
    }

    private TranscriptTurn hasStatus(RequirementStatus expected) {
      assertThat(result.state().status()).isEqualTo(expected);
      return this;
    }

    private TranscriptTurn goalsMention(String... terms) {
      String haystack = flattenedGoals();
      for (String term : terms) {
        assertThat(haystack).contains(term.toLowerCase(Locale.ROOT));
      }
      return this;
    }

    private TranscriptTurn goalsMentionAnyOf(String... terms) {
      String haystack = flattenedGoals();
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
          (flattenedGoals() + "\n" + result.assistantReply()).toLowerCase(Locale.ROOT);
      assertThat(
              Arrays.stream(terms)
                  .map(term -> term.toLowerCase(Locale.ROOT))
                  .anyMatch(haystack::contains))
          .isTrue();
      return this;
    }

    private TranscriptTurn assistantLooksLikeQuestion() {
      assertThat(result.assistantReply()).contains("?");
      return this;
    }

    private TranscriptTurn markdownHasRequiredSections() {
      String markdown = renderer.render(result.state());
      assertThat(markdown).contains("## Intent");
      assertThat(markdown).contains("## Minimum Requirements");
      assertThat(markdown).contains("## Constraints");
      assertThat(markdown).contains("## Missing Information");
      assertThat(markdown).contains("## Assumptions");
      assertThat(markdown).contains("## Status");
      return this;
    }

    private String flattenedGoals() {
      AgentState state = result.state();
      UserGoals userGoals = state.userGoals().orElseThrow();
      return String.join(
              "\n",
              userGoals.getIntent(),
              String.valueOf(userGoals.getDate()),
              String.valueOf(userGoals.getTime()),
              String.valueOf(userGoals.getPartySize()),
              String.join("\n", userGoals.getConstraints()),
              String.join("\n", state.missingInformation()),
              String.join("\n", state.assumptions()))
          .toLowerCase(Locale.ROOT);
    }
  }
}
