package com.example.jarvis.decisionsupport;

/**
 * Structured response from the decision support model.
 *
 * @param action one of "answer" (question about options), "restart" (user wants to change
 *     requirements), or "selected" (user picked a restaurant)
 * @param reply the natural language response to show the user
 */
public record DecisionSupportResponse(String action, String reply) {}
