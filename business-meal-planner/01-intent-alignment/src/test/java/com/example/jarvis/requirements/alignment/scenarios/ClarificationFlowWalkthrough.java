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
 * Student walkthrough: starting with a vague request and going through clarification. Set
 * breakpoints after each processMessage call to inspect the context, or just run the test and read
 * the console output.
 *
 * <pre>
 * mvn test -Dgroups=integration -Dtest=ClarificationFlowWalkthrough
 * </pre>
 */
@SpringBootTest(
    classes = IntentAlignmentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
class ClarificationFlowWalkthrough {

  @Autowired private RequirementsAligner aligner;

  @Test
  void vagueRequestToClarificationToConfirmation() {
    var context = new JarvisAgentContext();
    var history = new ArrayList<AgentMessage>();

    // Turn 1: Vague request — agent should ask for missing details
    var result1 = aligner.processMessage(context, "Help me plan a business meal.", history);
    history.add(new AgentMessage(Instant.now(), Role.USER, "Help me plan a business meal."));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, result1.assistantReply()));
    printTurn(1, context, result1);

    // Turn 2: Provide the details the agent asked about
    var result2 =
        aligner.processMessage(
            context,
            """
            It's an internal team lunch on April 20th at noon for 6 people.
            One person is gluten-free. I only want recommendations, no booking.
            """,
            history);
    history.add(new AgentMessage(Instant.now(), Role.USER, "It's an internal team lunch..."));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, result2.assistantReply()));
    printTurn(2, context, result2);

    // Turn 3: Confirm
    var result3 = aligner.processMessage(context, "exactly", history);
    history.add(new AgentMessage(Instant.now(), Role.USER, "exactly"));
    history.add(new AgentMessage(Instant.now(), Role.ASSISTANT, result3.assistantReply()));
    printTurn(3, context, result3);
  }

  private void printTurn(int turn, JarvisAgentContext context, RequirementsAligner.Result result) {
    System.out.println("\n=== Turn " + turn + " ===");
    System.out.println("Status: " + context.getStatus().label());
    System.out.println("Assistant: " + result.assistantReply());
    System.out.println("State:\n" + context.toMarkdown());
  }
}
