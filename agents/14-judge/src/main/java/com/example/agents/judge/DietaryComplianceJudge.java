package com.example.agents.judge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springaicommunity.judge.DeterministicJudge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Check;
import org.springaicommunity.judge.result.Judgment;
import org.springaicommunity.judge.result.JudgmentStatus;

/**
 * T1 deterministic judge: verifies the recommended restaurant meets stated dietary requirements.
 * Abstains if no dietary requirement was specified in the goal.
 */
public class DietaryComplianceJudge extends DeterministicJudge {

  public DietaryComplianceJudge() {
    super("DietaryCompliance", "Checks restaurant dietary options against user requirements");
  }

  @Override
  public Judgment judge(JudgmentContext context) {
    String goal = context.goal();
    String output = context.agentOutput().orElse("");
    List<Check> checks = new ArrayList<>();

    // Check if dietary requirements were mentioned in the goal
    boolean vegetarianRequested = goal != null && goal.toLowerCase().contains("vegetarian");

    if (!vegetarianRequested) {
      return Judgment.abstain("No dietary requirements specified in goal");
    }

    checks.add(Check.pass("dietary-requested", "Vegetarian requirement detected in goal"));

    // Find the recommended restaurant
    Map<String, Object> matched = null;
    for (Map<String, Object> r : RestaurantTools.RESTAURANTS) {
      if (output.toLowerCase().contains(r.get("name").toString().toLowerCase())) {
        matched = r;
        break;
      }
    }

    if (matched == null) {
      checks.add(Check.fail("restaurant-identified", "Could not identify a restaurant in output"));
      return Judgment.builder()
          .status(JudgmentStatus.FAIL)
          .reasoning("No known restaurant found in agent output")
          .checks(checks)
          .build();
    }

    String name = matched.get("name").toString();
    boolean hasVegetarian = (boolean) matched.get("vegetarianOptions");

    if (hasVegetarian) {
      checks.add(Check.pass("dietary-met", name + " has vegetarian options"));
      return Judgment.builder()
          .status(JudgmentStatus.PASS)
          .reasoning(name + " has vegetarian options")
          .checks(checks)
          .build();
    } else {
      checks.add(Check.fail("dietary-met", name + " does NOT have vegetarian options"));
      return Judgment.builder()
          .status(JudgmentStatus.FAIL)
          .reasoning(name + " lacks vegetarian options")
          .checks(checks)
          .build();
    }
  }
}
