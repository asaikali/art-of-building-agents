package com.example.jarvis.planning.scenarios;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.RestaurantPlanningApplication;
import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.planning.RestaurantPlanner;
import com.example.jarvis.requirements.alignment.AlignmentStatus;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * End-to-end walkthrough: alignment through planning. Reproduces the inspector session of a
 * vegetarian dinner for 2 at 6pm with a $100 budget. Set breakpoints or read the console output.
 *
 * <pre>
 * mvn test -Dgroups=integration -Dtest=VegetarianDinnerWalkthrough
 * </pre>
 */
@SpringBootTest(
    classes = RestaurantPlanningApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
class VegetarianDinnerWalkthrough {

  @Autowired private RequirementsAligner aligner;
  @Autowired private RestaurantPlanner planner;

  @Test
  void vegetarianDinnerForTwo() {
    var context = new JarvisAgentContext();

    // Turn 1: Greeting
    var result1 = sendMessage(context, "hi");
    printAlignmentTurn(1, context, result1);

    // Turn 2: Provide date, time, party size, meal type
    var result2 = sendMessage(context, "tomorrow at 6 pm for dinner for 2 people.");
    printAlignmentTurn(2, context, result2);

    // Turn 3: Add budget and dietary constraint
    var result3 = sendMessage(context, "budget is $100 and one person is vegetarian");
    printAlignmentTurn(3, context, result3);

    // Turn 4: Confirm requirements
    var result4 = sendMessage(context, "yep");
    printAlignmentTurn(4, context, result4);

    // Requirements should now be confirmed — run planning
    if (context.getAlignmentStatus() == AlignmentStatus.REQUIREMENTS_CONFIRMED) {
      System.out.println("\n=== Planning ===");
      System.out.println(
          "Requirements:\n" + JsonUtils.toJson(context.getUserRequirements()) + "\n");

      String planningResult = planner.plan(context.getUserRequirements());
      System.out.println("Planning Result:\n" + planningResult);
    } else {
      System.out.println(
          "\n⚠ Requirements not confirmed after 4 turns. Status: "
              + context.getAlignmentStatus().label());
    }
  }

  private RequirementsAligner.Result sendMessage(JarvisAgentContext context, String userMessage) {
    var result =
        aligner.processMessage(
            context.getUserRequirements(), context.getAlignmentStatus(), userMessage);
    context.setUserRequirements(result.updatedRequirements());
    context.setAlignmentStatus(result.updatedStatus());
    return result;
  }

  private void printAlignmentTurn(
      int turn, JarvisAgentContext context, RequirementsAligner.Result result) {
    System.out.println("\n=== Turn " + turn + " ===");
    System.out.println("Status: " + context.getAlignmentStatus().label());
    System.out.println("Assistant: " + result.reply());
    System.out.println(
        "State:\n"
            + JsonUtils.toJson(
                Map.of(
                    "requirements", context.getUserRequirements(),
                    "status", context.getAlignmentStatus())));
  }
}
