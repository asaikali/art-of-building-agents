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
    var reply1 =
        aligner.processMessage(
            context,
            "I have a client dinner tomorrow at 7pm for 4 people, one is vegetarian.",
            history);
    history.add(new AgentMessage(Instant.now(), Role.USER, "I have a client dinner..."));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, reply1));
    printTurn(1, context, reply1);

    // Turn 2: Add budget and origin
    var reply2 =
        aligner.processMessage(
            context, "I'm leaving from Union Station. Keep it under 120 CAD per person.", history);
    history.add(new AgentMessage(Instant.now(), Role.USER, "I'm leaving from Union Station..."));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, reply2));
    printTurn(2, context, reply2);

    // Turn 3: Confirm
    var reply3 = aligner.processMessage(context, "yes", history);
    history.add(new AgentMessage(Instant.now(), Role.USER, "yes"));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, reply3));
    printTurn(3, context, reply3);
  }

  private void printTurn(int turn, JarvisAgentContext context, String reply) {
    System.out.println("\n=== Turn " + turn + " ===");
    System.out.println("Status: " + context.getStatus().label());
    System.out.println("Assistant: " + reply);
    System.out.println("State:\n" + context.toMarkdown());
  }
}
