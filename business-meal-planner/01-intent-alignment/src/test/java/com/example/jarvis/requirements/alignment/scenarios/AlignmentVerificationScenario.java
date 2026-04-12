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

    var correction =
        processMessage(
            context,
            "Actually make it lunch, not dinner. Budget is 80 per person.",
            conversationHistory);
    assertThat(correction.state().getStatus())
        .isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(stateJson(correction)).contains("lunch", "80");

    var confirmation = processMessage(context, "yes", conversationHistory);
    assertThat(confirmation.state().getStatus())
        .isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
  }

  @Test
  @DisplayName("Vague start, then clarification, then confirmation")
  void vagueStartThenClarificationThenConfirmation() {
    var context = new JarvisAgentContext();
    var conversationHistory = new ArrayList<AgentMessage>();

    var vague = processMessage(context, "Help me plan a business meal.", conversationHistory);
    assertThat(vague.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);

    var result =
        processMessage(
            context,
            "Team lunch on April 20th at noon for 6 people, one gluten-free.",
            conversationHistory);
    assertThat(result.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(stateJson(result)).contains("lunch", "6");

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
