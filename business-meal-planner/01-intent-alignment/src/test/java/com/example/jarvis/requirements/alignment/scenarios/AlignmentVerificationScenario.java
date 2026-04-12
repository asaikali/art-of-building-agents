package com.example.jarvis.requirements.alignment.scenarios;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.IntentAlignmentApplication;
import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.requirements.alignment.RequirementStatus;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    classes = IntentAlignmentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
class AlignmentVerificationScenario {

  @Autowired private RequirementsAligner aligner;

  @Test
  @DisplayName("Initial request, then correction, then confirmation")
  void initialRequestThenCorrectionThenConfirmation() {
    var context = new JarvisAgentContext();
    var conversationHistory = new ArrayList<AgentMessage>();

    // User provides a detailed initial request with enough info (date, time, party size)
    // for the aligner to move straight to confirmation
    var initial =
        processMessage(
            context,
            """
            I have a client dinner tomorrow at 7 pm for 4 people.
            One guest is vegetarian. I want somewhere quiet.
            """,
            conversationHistory);
    assertThat(initial.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(stateJson(initial)).contains("dinner", "vegetarian");

    // User corrects the meal type and adds budget — the aligner should update
    // the requirements and ask for confirmation again (not confirm automatically)
    var correction =
        processMessage(
            context,
            "Actually make it lunch, not dinner. Budget is 80 per person.",
            conversationHistory);
    assertThat(correction.state().getStatus())
        .isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(stateJson(correction)).contains("lunch", "80");

    // User confirms — the aligner detects that the requirements are unchanged
    // from the previous turn and marks them as confirmed
    var confirmation = processMessage(context, "yes", conversationHistory);
    assertThat(confirmation.state().getStatus())
        .isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
  }

  @Test
  @DisplayName("Vague start, then clarification, then confirmation")
  void vagueStartThenClarificationThenConfirmation() {
    var context = new JarvisAgentContext();
    var conversationHistory = new ArrayList<AgentMessage>();

    // User starts with a vague request that lacks required fields (date, time, party size).
    // The aligner should ask for clarification instead of moving to confirmation.
    var vague = processMessage(context, "Help me plan a business meal.", conversationHistory);
    assertThat(vague.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);

    // User provides the missing details. The aligner should now have enough
    // information to move to confirmation.
    var result =
        processMessage(
            context,
            "Team lunch on April 20th at noon for 6 people, one gluten-free.",
            conversationHistory);
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(stateJson(result)).contains("lunch", "6");

    // User confirms the requirements
    var confirmation = processMessage(context, "looks good", conversationHistory);
    assertThat(confirmation.state().getStatus())
        .isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
  }

  private RequirementsAligner.Result processMessage(
      JarvisAgentContext context, String userMessage, List<AgentMessage> history) {
    var result = aligner.processMessage(context, userMessage, history);
    history.add(new AgentMessage(Instant.now(), Role.USER, userMessage));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, result.assistantReply()));
    return result;
  }

  private String stateJson(RequirementsAligner.Result result) {
    return JsonUtils.toJson(result.state().getUserRequirements()).toLowerCase(Locale.ROOT);
  }
}
