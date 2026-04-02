package com.example.agents.workflow;

import java.util.List;
import java.util.Map;

/** Output of the dinner planning workflow. */
public record DinnerResult(
    List<Map<String, Object>> candidates,
    String recommendation,
    boolean valid,
    String validationMessage) {}
