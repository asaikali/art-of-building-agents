package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.session.SessionId;
import com.example.jarvis.IntentAlignmentApplication;
import com.example.jarvis.agent.AgentState;
import com.example.jarvis.agent.RequirementStatus;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.EventRequirements;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = IntentAlignmentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfSystemProperty(named = "jarvis.openai.integration", matches = "true")
class RequirementsAlignmentLoopIntegrationTest {

  @Autowired private RequirementsAlignmentLoop alignmentLoop;

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
        .stateMentions("dinner", "vegetarian", "quiet")
        .assistantMentionsAnyOf("confirm", "correct")
        .markdownHasRequiredSections();

    scenario
        .user("Don't book yet. I'm leaving from Union Station. Keep it under 120 CAD per person.")
        .hasEventName("plan-updated")
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .stateMentions("120")
        .stateMentionsAnyOf("union", "station")
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
        .stateMentionsAnyOf("quiet", "low noise", "conversation", "client")
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
        .stateMentions("lunch", "6", "gluten")
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
      return new TranscriptTurn(alignmentLoop.handleTurn(sessionId, text));
    }
  }

  private final class TranscriptTurn {

    private final RequirementsAlignmentLoop.TurnResult result;

    private TranscriptTurn(RequirementsAlignmentLoop.TurnResult result) {
      this.result = result;
    }

    private TranscriptTurn hasEventName(String expected) {
      assertThat(result.eventName()).isEqualTo(expected);
      return this;
    }

    private TranscriptTurn hasStatus(RequirementStatus expected) {
      assertThat(result.state().getStatus()).isEqualTo(expected);
      return this;
    }

    private TranscriptTurn stateMentions(String... terms) {
      String haystack = flattenedState();
      for (String term : terms) {
        assertThat(haystack).contains(term.toLowerCase(Locale.ROOT));
      }
      return this;
    }

    private TranscriptTurn stateMentionsAnyOf(String... terms) {
      String haystack = flattenedState();
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
          (flattenedState() + "\n" + result.assistantReply()).toLowerCase(Locale.ROOT);
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
      String markdown = result.state().toMarkdown();
      assertThat(markdown).contains("## Event Requirements");
      assertThat(markdown).contains("## Additional Requirements");
      assertThat(markdown).contains("## Cuisine Preferences");
      assertThat(markdown).contains("## Attendees");
      assertThat(markdown).contains("## Missing Information");
      assertThat(markdown).contains("## Status");
      return this;
    }

    private String flattenedState() {
      AgentState state = result.state();
      EventRequirements eventRequirements = state.getEventRequirements();
      String attendees =
          state.getAttendees().stream()
              .map(this::flattenAttendee)
              .collect(java.util.stream.Collectors.joining("\n"));
      return String.join(
              "\n",
              String.valueOf(eventRequirements == null ? null : eventRequirements.getDate()),
              String.valueOf(eventRequirements == null ? null : eventRequirements.getTime()),
              String.valueOf(eventRequirements == null ? null : eventRequirements.getPartySize()),
              String.valueOf(eventRequirements == null ? null : eventRequirements.getMealType()),
              String.valueOf(eventRequirements == null ? null : eventRequirements.getPurpose()),
              String.valueOf(
                  eventRequirements == null ? null : eventRequirements.getBudgetPerPerson()),
              String.valueOf(eventRequirements == null ? null : eventRequirements.getNoiseLevel()),
              eventRequirements == null
                  ? ""
                  : String.join("\n", eventRequirements.getAdditionalRequirements()),
              eventRequirements == null
                  ? ""
                  : String.join("\n", eventRequirements.getCuisinePreferences()),
              attendees,
              String.join("\n", state.getMissingInformation()))
          .toLowerCase(Locale.ROOT);
    }

    private String flattenAttendee(Attendee attendee) {
      return String.join(
          "\n",
          String.valueOf(attendee.getName()),
          String.valueOf(attendee.getOrigin()),
          String.valueOf(attendee.getDepartureTime()),
          String.valueOf(attendee.getTravelMode()),
          String.valueOf(attendee.getMaxTravelTimeMinutes()),
          String.valueOf(attendee.getMaxDistanceKm()),
          attendee.getDietaryConstraints().stream()
              .map(Enum::name)
              .collect(java.util.stream.Collectors.joining("\n")));
    }
  }
}
