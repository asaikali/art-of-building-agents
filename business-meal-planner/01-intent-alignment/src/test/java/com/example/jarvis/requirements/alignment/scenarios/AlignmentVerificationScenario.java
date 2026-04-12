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
  void richInputThenCorrectionThenConfirmation() {
    var context = new JarvisAgentContext();
    var history = new ArrayList<AgentMessage>();

    // Turn 1: Rich initial request
    var turn1 =
        process(
            context,
            history,
            """
            I have a client dinner tomorrow at 7 pm for 4 people.
            One guest is vegetarian. I want somewhere quiet.
            """);
    assertThat(turn1.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(stateJson(turn1)).contains("dinner", "vegetarian");

    // Turn 2: Correction — change to lunch, add budget
    var turn2 =
        process(context, history, "Actually make it lunch, not dinner. Budget is 80 per person.");
    assertThat(turn2.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(stateJson(turn2)).contains("lunch", "80");

    // Turn 3: Confirm
    var turn3 = process(context, history, "yes");
    assertThat(turn3.state().getStatus()).isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
  }

  @Test
  void vagueStartThenClarificationThenConfirmation() {
    var context = new JarvisAgentContext();
    var history = new ArrayList<AgentMessage>();

    // Turn 1: Vague request
    var turn1 = process(context, history, "Help me plan a business meal.");
    assertThat(turn1.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CLARIFICATION);

    // Turn 2: Provide details
    var turn2 =
        process(
            context, history, "Team lunch on April 20th at noon for 6 people, one gluten-free.");
    assertThat(turn2.state().getStatus()).isEqualTo(RequirementStatus.WAITING_FOR_CONFIRMATION);
    assertThat(stateJson(turn2)).contains("lunch", "6");

    // Turn 3: Confirm
    var turn3 = process(context, history, "looks good");
    assertThat(turn3.state().getStatus()).isEqualTo(RequirementStatus.REQUIREMENTS_CONFIRMED);
  }

  private RequirementsAligner.Result process(
      JarvisAgentContext context, List<AgentMessage> history, String userMessage) {
    var result = aligner.processMessage(context, userMessage, history);
    history.add(new AgentMessage(Instant.now(), Role.USER, userMessage));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, result.assistantReply()));
    return result;
  }

  private String stateJson(RequirementsAligner.Result result) {
    return JsonUtils.toJson(result.state().getUserRequirements()).toLowerCase(Locale.ROOT);
  }
}
