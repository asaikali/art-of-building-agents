package com.example.jarvis.requirements.alignment.scenarios;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.DecisionSupportApplication;
import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.requirements.alignment.AlignmentStatus;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verification scenario for coding agents. Runs a multi-turn conversation against the real model
 * and asserts on status and captured state per turn. Run with:
 *
 * <pre>
 * mvn test -Dgroups=integration -Dtest=AlignmentVerificationScenario
 * </pre>
 */
@SpringBootTest(
    classes = DecisionSupportApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
class AlignmentVerificationScenario {

  @Autowired private RequirementsAligner aligner;

  @Test
  @DisplayName("Initial request, then correction, then confirmation")
  void initialRequestThenCorrectionThenConfirmation() {
    var context = new JarvisAgentContext();

    // User provides a detailed initial request with enough info (date, time, party size)
    // for the aligner to move straight to confirming
    sendMessage(
        context,
        """
        I have a client dinner tomorrow at 7 pm for 4 people.
        One guest is vegetarian. I want somewhere quiet.
        """);
    assertThat(context.getAlignmentStatus()).isEqualTo(AlignmentStatus.CONFIRMING_REQUIREMENTS);
    assertThat(stateJson(context)).contains("dinner", "vegetarian");

    // User corrects the meal type and adds budget — the aligner should update
    // the requirements and ask for confirmation again (not confirm automatically)
    sendMessage(context, "Actually make it lunch, not dinner. Budget is 80 per person.");
    assertThat(context.getAlignmentStatus()).isEqualTo(AlignmentStatus.CONFIRMING_REQUIREMENTS);
    assertThat(stateJson(context)).contains("lunch", "80");

    // User confirms — the aligner detects that the requirements are unchanged
    // from the previous turn and marks them as confirmed
    sendMessage(context, "yes");
    assertThat(context.getAlignmentStatus()).isEqualTo(AlignmentStatus.REQUIREMENTS_CONFIRMED);
  }

  @Test
  @DisplayName("Vague start, then gathering, then confirming")
  void vagueStartThenGatheringThenConfirming() {
    var context = new JarvisAgentContext();

    // User starts with a vague request that lacks required fields (date, time, party size).
    // The aligner should stay in gathering instead of moving to confirming.
    sendMessage(context, "Help me plan a business meal.");
    assertThat(context.getAlignmentStatus()).isEqualTo(AlignmentStatus.GATHERING_REQUIREMENTS);

    // User provides the missing details. The aligner should now have enough
    // information to move to confirming.
    sendMessage(context, "Team lunch on April 20th at noon for 6 people, one gluten-free.");
    assertThat(context.getAlignmentStatus()).isEqualTo(AlignmentStatus.CONFIRMING_REQUIREMENTS);
    assertThat(stateJson(context)).contains("lunch", "6");

    // User confirms the requirements
    sendMessage(context, "looks good");
    assertThat(context.getAlignmentStatus()).isEqualTo(AlignmentStatus.REQUIREMENTS_CONFIRMED);
  }

  private void sendMessage(JarvisAgentContext context, String userMessage) {
    var result =
        aligner.processMessage(
            context.getUserRequirements(), context.getAlignmentStatus(), userMessage);
    context.setUserRequirements(result.updatedRequirements());
    context.setAlignmentStatus(result.updatedStatus());
  }

  private String stateJson(JarvisAgentContext context) {
    return JsonUtils.toJson(context.getUserRequirements()).toLowerCase(Locale.ROOT);
  }
}
