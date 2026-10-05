package com.example.jarvis.constraints.deterministic.budget;

/** Verdict from {@link BudgetPerPersonCheck} with a short rationale. */
public record BudgetPerPersonCheckResult(BudgetPerPersonCheckStatus status, String rationale) {}
