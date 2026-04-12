package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.jarvis.IntentAlignmentApplication;
import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.UserRequirements;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
class RequirementsAlignerIntegrationTest {

  @Autowired private RequirementsAligner aligner;

  @Test
  void supportsLongBackAndForthTranscriptAgainstConfiguredModel() {
    TranscriptScenario scenario = scenario();

    scenario
        .user(
            """
            I have a client dinner tomorrow at 7 pm for 4 people. One guest is vegetarian.
            I want somewhere professional and quiet enough to talk.
            """)
        .hasEventName("plan-updated")
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .stateMentions("dinner", "vegetarian", "quiet")
        .assistantMentionsAnyOf("confirm", "correct", "look")
        .markdownHasRequiredSections();

    scenario
        .user("Don't book yet. I'm leaving from Union Station. Keep it under 120 CAD per person.")
        .hasEventName("plan-updated")
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .stateMentions("120")
        .stateMentionsAnyOf("union", "station")
        .assistantMentionsAnyOf("confirm", "correct", "look");

    scenario
        .user("Let's optimize for low noise and easy conversation with a client.")
        .hasEventName("plan-updated")
        .hasStatus(RequirementStatus.WAITING_FOR_CONFIRMATION)
        .stateMentionsAnyOf("quiet", "low noise", "conversation", "client")
        .assistantMentionsAnyOf("confirm", "correct", "look");

    scenario
        .user("yes")
        .hasEventName("requirements-confirmed")
        .hasStatus(RequirementStatus.REQUIREMENTS_CONFIRMED)
        .assistantMentionsAnyOf("confirmed", "all set", "locked in", "ready");
  }

  @Test
  void supportsClarifyThenConfirmTranscriptAgainstConfiguredModel() {
    TranscriptScenario scenario = scenario();

    scenario
        .user("Help me plan a business meal.")
        .hasEventName("clarification-requested")
        .hasStatus(RequirementStatus.WAITING_FOR_CLARIFICATION)
        .assistantMentionsAnyOf("date", "time", "when", "party")
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
        .assistantMentionsAnyOf("confirm", "correct", "look");

    scenario
        .user("exactly")
        .hasEventName("requirements-confirmed")
        .hasStatus(RequirementStatus.REQUIREMENTS_CONFIRMED)
        .assistantMentionsAnyOf("confirmed", "all set", "locked in", "ready");
  }

  private TranscriptScenario scenario() {
    return new TranscriptScenario();
  }

  private final class TranscriptScenario {

    private final JarvisAgentContext state = new JarvisAgentContext();
    private final List<AgentMessage> conversationHistory = new ArrayList<>();

    private TranscriptTurn user(String text) {
      RequirementsAligner.Result result = aligner.processMessage(state, text, conversationHistory);
      conversationHistory.add(new AgentMessage(java.time.Instant.now(), Role.USER, text));
      conversationHistory.add(
          new AgentMessage(java.time.Instant.now(), Role.ASSISTANT, result.assistantReply()));
      return new TranscriptTurn(result);
    }
  }

  private final class TranscriptTurn {

    private final RequirementsAligner.Result result;

    private TranscriptTurn(RequirementsAligner.Result result) {
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

    private TranscriptTurn markdownHasRequiredSections() {
      String markdown = result.state().toMarkdown();
      assertThat(markdown).contains("## Meal");
      assertThat(markdown).contains("## Additional Requirements");
      assertThat(markdown).contains("## Cuisine Preferences");
      assertThat(markdown).contains("## Attendees");
      assertThat(markdown).contains("## Missing Information");
      assertThat(markdown).contains("## Status");
      return this;
    }

    private String flattenedState() {
      JarvisAgentContext state = result.state();
      UserRequirements userRequirements = state.getUserRequirements();
      Meal meal = userRequirements.getMeal();
      String attendees =
          userRequirements.getAttendees().stream()
              .map(this::flattenAttendee)
              .collect(java.util.stream.Collectors.joining("\n"));
      return String.join(
              "\n",
              String.valueOf(meal == null ? null : meal.getDate()),
              String.valueOf(meal == null ? null : meal.getTime()),
              String.valueOf(meal == null ? null : meal.getPartySize()),
              String.valueOf(meal == null ? null : meal.getMealType()),
              String.valueOf(meal == null ? null : meal.getPurpose()),
              String.valueOf(meal == null ? null : meal.getBudgetPerPerson()),
              String.valueOf(meal == null ? null : meal.getNoiseLevel()),
              meal == null ? "" : String.join("\n", meal.getAdditionalRequirements()),
              meal == null ? "" : String.join("\n", meal.getCuisinePreferences()),
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
