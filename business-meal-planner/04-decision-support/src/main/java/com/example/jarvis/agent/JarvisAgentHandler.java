package com.example.jarvis.agent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.json.JsonUtils;
import com.example.agent.core.session.Session;
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

  public JarvisAgentHandler(
      RequirementsAligner requirementsAligner, RestaurantPlanner restaurantPlanner) {
    this.requirementsAligner = requirementsAligner;
    this.restaurantPlanner = restaurantPlanner;
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
        "onMessage | status={} | user=\"{}\"",
        context.getAlignmentStatus().label(),
        message.text());

    handleAlignment(session, context, message);

    // If alignment just confirmed, immediately start planning
    if (context.getAlignmentStatus() == AlignmentStatus.REQUIREMENTS_CONFIRMED) {
      handlePlanning(session, context);
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

    String reply = restaurantPlanner.plan(context.getUserRequirements());

    // Reset to gathering so the next message goes through alignment
    // (user might want to relax constraints and try again)
    context.setAlignmentStatus(AlignmentStatus.GATHERING_REQUIREMENTS);

    log.info("planning done | reply length={}", reply.length());

    session.reply(reply);
    updateInspectorState(session, context, "Planning complete", reply);
    session.logEvent("planning-completed", Map.of());
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
