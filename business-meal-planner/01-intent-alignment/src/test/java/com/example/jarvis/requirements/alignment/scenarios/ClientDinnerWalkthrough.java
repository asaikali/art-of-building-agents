package com.example.jarvis.requirements.alignment.scenarios;

import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.jarvis.IntentAlignmentApplication;
import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Student walkthrough: planning a client dinner. Set breakpoints after each processMessage call to
 * inspect the context, or just run the test and read the console output.
 *
 * <pre>
 * mvn test -Dgroups=integration -Dtest=ClientDinnerWalkthrough
 * </pre>
 */
@SpringBootTest(
    classes = IntentAlignmentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
class ClientDinnerWalkthrough {

  @Autowired private RequirementsAligner aligner;

  @Test
  void clientDinnerForFour() {
    var context = new JarvisAgentContext();
    var history = new ArrayList<AgentMessage>();

    // Turn 1: Describe the meal
    var result1 =
        aligner.processMessage(
            context.getUserRequirements(),
            context.getStatus(),
            "I have a client dinner tomorrow at 7pm for 4 people, one is vegetarian.",
            history);
    applyResult(context, result1);
    history.add(new AgentMessage(Instant.now(), Role.USER, "I have a client dinner..."));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, result1.reply()));
    printTurn(1, context, result1.reply());

    // Turn 2: Add budget and origin
    var result2 =
        aligner.processMessage(
            context.getUserRequirements(),
            context.getStatus(),
            "I'm leaving from Union Station. Keep it under 120 CAD per person.",
            history);
    applyResult(context, result2);
    history.add(new AgentMessage(Instant.now(), Role.USER, "I'm leaving from Union Station..."));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, result2.reply()));
    printTurn(2, context, result2.reply());

    // Turn 3: Confirm
    var result3 =
        aligner.processMessage(context.getUserRequirements(), context.getStatus(), "yes", history);
    applyResult(context, result3);
    history.add(new AgentMessage(Instant.now(), Role.USER, "yes"));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, result3.reply()));
    printTurn(3, context, result3.reply());
  }

  private void applyResult(JarvisAgentContext context, RequirementsAligner.Result result) {
    context.setUserRequirements(result.updatedRequirements());
    context.setMissingInformation(result.check().missingCriticalFields());
    context.setStatus(result.check().status());
  }

  private void printTurn(int turn, JarvisAgentContext context, String reply) {
    System.out.println("\n=== Turn " + turn + " ===");
    System.out.println("Status: " + context.getStatus().label());
    System.out.println("Assistant: " + reply);
    System.out.println("State:\n" + context.toMarkdown());
  }
}
