package com.example.agents.trajectory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springaicommunity.judge.DeterministicJudge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Check;
import org.springaicommunity.judge.result.Judgment;
import org.springaicommunity.judge.result.JudgmentStatus;

/** T0 deterministic judge: verifies the recommended restaurant is within expense policy. */
public class ExpensePolicyJudge extends DeterministicJudge {

  private static final double EXPENSE_LIMIT = 50.0;

  public ExpensePolicyJudge() {
    super("ExpensePolicy", "Checks restaurant price against corporate expense limit");
  }

  @Override
  public Judgment judge(JudgmentContext context) {
    String output = context.agentOutput().orElse("");
    List<Check> checks = new ArrayList<>();

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
    int price = (int) matched.get("pricePerPerson");
    checks.add(Check.pass("restaurant-identified", "Found: " + name));

    if (price <= EXPENSE_LIMIT) {
      checks.add(
          Check.pass(
              "within-budget",
              String.format(
                  "%s at \u20ac%d/person within \u20ac%.0f limit", name, price, EXPENSE_LIMIT)));
      return Judgment.builder()
          .status(JudgmentStatus.PASS)
          .reasoning(String.format("Price \u20ac%d within \u20ac%.0f limit", price, EXPENSE_LIMIT))
          .checks(checks)
          .build();
    } else {
      checks.add(
          Check.fail(
              "within-budget",
              String.format(
                  "%s at \u20ac%d/person exceeds \u20ac%.0f limit", name, price, EXPENSE_LIMIT)));
      return Judgment.builder()
          .status(JudgmentStatus.FAIL)
          .reasoning(String.format("Price \u20ac%d exceeds \u20ac%.0f limit", price, EXPENSE_LIMIT))
          .checks(checks)
          .build();
    }
  }
}
