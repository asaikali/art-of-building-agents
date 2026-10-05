package com.example.jarvis.agent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.session.Session;
import com.example.jarvis.decisionsupport.DecisionSupport;
import com.example.jarvis.planning.RestaurantPlanner;
import com.example.jarvis.requirements.alignment.AlignmentStatus;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JarvisAgentHandler implements AgentHandler {

  private final RequirementsAligner requirementsAligner;
  private final RestaurantPlanner restaurantPlanner;
  private final DecisionSupport decisionSupport;

  public JarvisAgentHandler(
      RequirementsAligner requirementsAligner,
      RestaurantPlanner restaurantPlanner,
      DecisionSupport decisionSupport) {
    this.requirementsAligner = requirementsAligner;
    this.restaurantPlanner = restaurantPlanner;
    this.decisionSupport = decisionSupport;
  }

  @Override
  public String getName() {
    return "Jarvis";
  }

  @Override
  public String getInitialAssistantMessage() {
    return """
        Hi, I'm Jarvis 👋

        I specialize in planning memorable dining experiences.
        Tell me about the occasion, and I'll help you design something just right.
        """;
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    var context = session.getOrCreateContext(JarvisAgentContext.class, JarvisAgentContext::new);

    if (context.getPhase() == WorkflowPhase.EXPLORING_OPTIONS) {
      handleDecisionSupport(session, context, message);
    } else {
      handleAlignment(session, context, message);

      // If alignment just confirmed, immediately start planning
      if (context.getAlignmentStatus() == AlignmentStatus.REQUIREMENTS_CONFIRMED) {
        handlePlanning(session, context);
      }
    }
  }

  private void handleAlignment(Session session, JarvisAgentContext context, AgentMessage message) {
    var result =
        requirementsAligner.processMessage(
            context.getUserRequirements(), context.getAlignmentStatus(), message.text());

    context.setUserRequirements(result.updatedRequirements());
    context.setAlignmentStatus(result.updatedStatus());

    session.reply(result.reply());
    session.updateState(InspectorState.render(context, context.getAlignmentStatus().label(), null));
    session.logEvent(
        context.getAlignmentStatus().label(),
        Map.of("missingFieldCount", result.missingRequiredFields().size()));
  }

  private void handlePlanning(Session session, JarvisAgentContext context) {
    session.logEvent("planning-started", Map.of());
    session.updateState(
        InspectorState.render(context, "Planning: Searching for restaurants...", null));

    String shortlist = restaurantPlanner.plan(context.getUserRequirements());

    // Store shortlist and move to exploring options
    context.setShortlist(shortlist);
    context.setPhase(WorkflowPhase.EXPLORING_OPTIONS);

    session.reply(shortlist);
    session.updateState(InspectorState.render(context, "Exploring options", shortlist));
    session.logEvent("planning-completed", Map.of());
  }

  private void handleDecisionSupport(
      Session session, JarvisAgentContext context, AgentMessage message) {
    session.logEvent("decision-support-query", Map.of("text", message.text()));

    var response =
        decisionSupport.ask(context.getUserRequirements(), context.getShortlist(), message.text());

    session.reply(response.reply());

    switch (response.action()) {
      case "restart" -> {
        // User wants to change requirements — go back to alignment
        context.returnToAlignment();
        session.updateState(
            InspectorState.render(context, "Restarting: " + response.action(), null));
      }
      case "selected" -> {
        session.logEvent("restaurant-booked", Map.of("reply", response.reply()));
        // Reset to alignment so the user can plan another meal
        context.returnToAlignment();
        session.updateState(InspectorState.render(context, "Restaurant booked", null));
      }
      default -> {
        // "answer" or any other action — stay in exploring options
        session.updateState(
            InspectorState.render(context, "Exploring options", context.getShortlist()));
      }
    }

    session.logEvent("decision-support-response", Map.of("action", response.action()));
  }
}
