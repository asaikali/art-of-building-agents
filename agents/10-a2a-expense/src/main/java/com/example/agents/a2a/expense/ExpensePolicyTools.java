package com.example.agents.a2a.expense;

import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Corporate expense policy tool — the same deterministic logic as previous steps, but now served
 * via A2A protocol instead of being called directly.
 */
@Service
public class ExpensePolicyTools {

  private static final double CORPORATE_EXPENSE_LIMIT_PER_PERSON = 50.0;

  @Tool(
      description =
          "Check if a restaurant's price per person is within the corporate expense policy limit")
  public Map<String, Object> checkExpensePolicy(
      @ToolParam(description = "Price per person in EUR") double pricePerPerson,
      @ToolParam(description = "Number of guests") int partySize) {
    double totalCost = pricePerPerson * partySize;
    double totalLimit = CORPORATE_EXPENSE_LIMIT_PER_PERSON * partySize;
    boolean withinPolicy = pricePerPerson <= CORPORATE_EXPENSE_LIMIT_PER_PERSON;
    return Map.of(
        "withinPolicy", withinPolicy,
        "pricePerPerson", pricePerPerson,
        "limitPerPerson", CORPORATE_EXPENSE_LIMIT_PER_PERSON,
        "totalCost", totalCost,
        "totalLimit", totalLimit);
  }
}
