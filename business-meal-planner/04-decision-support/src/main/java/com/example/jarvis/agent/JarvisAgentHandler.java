package com.example.jarvis.agent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.json.JsonUtils;
import com.example.agent.core.session.Session;
import com.example.jarvis.decisionsupport.DecisionSupport;
import com.example.jarvis.planning.RestaurantPlanner;
import com.example.jarvis.requirements.alignment.AlignmentStatus;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JarvisAgentHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(JarvisAgentHandler.class);

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

    log.info(
        "onMessage | phase={} | status={} | user=\"{}\"",
        context.getPhase(),
        context.getAlignmentStatus().label(),
        message.text());

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

    log.info(
        "alignment done | status={} | missingFields={}",
        result.updatedStatus().label(),
        result.missingRequiredFields().size());

    session.reply(result.reply());
    updateInspectorState(session, context, context.getAlignmentStatus().label(), null);
    session.logEvent(
        context.getAlignmentStatus().label(),
        Map.of("missingFieldCount", result.missingRequiredFields().size()));
  }

  private void handlePlanning(Session session, JarvisAgentContext context) {
    log.info("planning | starting restaurant search");
    session.logEvent("planning-started", Map.of());
    updateInspectorState(session, context, "Planning: Searching for restaurants...", null);

    String shortlist = restaurantPlanner.plan(context.getUserRequirements());

    // Store shortlist and move to exploring options
    context.setShortlist(shortlist);
    context.setPhase(WorkflowPhase.EXPLORING_OPTIONS);

    log.info("planning done | moving to EXPLORING_OPTIONS");

    session.reply(shortlist);
    updateInspectorState(session, context, "Exploring options", shortlist);
    session.logEvent("planning-completed", Map.of());
  }

  private void handleDecisionSupport(
      Session session, JarvisAgentContext context, AgentMessage message) {
    log.info("decision support | answering question");
    session.logEvent("decision-support-query", Map.of("text", message.text()));

    var response =
        decisionSupport.ask(context.getUserRequirements(), context.getShortlist(), message.text());

    session.reply(response.reply());

    switch (response.action()) {
      case "restart" -> {
        // User wants to change requirements — go back to alignment
        log.info("decision support | restart requested, resetting to alignment");
        context.setPhase(WorkflowPhase.ALIGNMENT);
        context.setAlignmentStatus(AlignmentStatus.GATHERING_REQUIREMENTS);
        context.setShortlist(null);
        updateInspectorState(session, context, "Restarting: " + response.action(), null);
      }
      case "selected" -> {
        log.info("decision support | restaurant selected");
        updateInspectorState(session, context, "Restaurant selected", context.getShortlist());
      }
      default -> {
        // "answer" or any other action — stay in exploring options
        updateInspectorState(session, context, "Exploring options", context.getShortlist());
      }
    }

    session.logEvent("decision-support-response", Map.of("action", response.action()));
  }

  private void updateInspectorState(
      Session session, JarvisAgentContext context, String status, String planningResult) {
    var state =
        """
        # Agent Context

        ## Requirements
        ```json
        %s
        ```

        ## Status
        %s
        """
            .formatted(JsonUtils.toJson(context.getUserRequirements()), status);

    if (planningResult != null) {
      state += "\n## Planning Result\n\n" + planningResult;
    }

    session.updateState(state);
  }
}
