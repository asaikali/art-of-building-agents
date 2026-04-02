package com.example.agents.workflow;

/** Input for the dinner planning workflow. */
public record DinnerRequest(
    String neighborhood, String cuisine, int partySize, double budgetPerPerson, String dietary) {}
