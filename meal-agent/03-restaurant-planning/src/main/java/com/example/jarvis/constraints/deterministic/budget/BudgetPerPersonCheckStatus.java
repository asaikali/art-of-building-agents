package com.example.jarvis.constraints.deterministic.budget;

/** Possible outcomes of {@link BudgetPerPersonCheck}. */
public enum BudgetPerPersonCheckStatus {
  /** Budget covers the restaurant's upper bound (clearly affordable). */
  PASS,
  /** Budget is below the restaurant's lower bound (clearly out of reach). */
  FAIL,
  /** Budget falls inside the restaurant's published range (could go either way). */
  MAYBE
}
