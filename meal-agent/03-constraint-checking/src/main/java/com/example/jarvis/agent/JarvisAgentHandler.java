package com.example.jarvis.agent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.json.JsonUtils;
import com.example.agent.core.session.Session;
import com.example.jarvis.requirements.alignment.AlignmentStatus;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import com.example.jarvis.search.RestaurantSearcher;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JarvisAgentHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(JarvisAgentHandler.class);

  private final RequirementsAligner requirementsAligner;
  private final RestaurantSearcher restaurantSearcher;

  public JarvisAgentHandler(
      RequirementsAligner requirementsAligner, RestaurantSearcher restaurantSearcher) {
    this.requirementsAligner = requirementsAligner;
    this.restaurantSearcher = restaurantSearcher;
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

    // If alignment just confirmed, immediately start searching
    if (context.getAlignmentStatus() == AlignmentStatus.REQUIREMENTS_CONFIRMED) {
      handleSearch(session, context);
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

  private void handleSearch(Session session, JarvisAgentContext context) {
    log.info("search | starting restaurant search");
    session.logEvent("search-started", Map.of());
    updateInspectorState(session, context, "Searching for available restaurants...", null);

    String reply = restaurantSearcher.search(context.getUserRequirements());

    // Reset to gathering so the next message goes through alignment
    // (user might want to relax constraints and try again)
    context.setAlignmentStatus(AlignmentStatus.GATHERING_REQUIREMENTS);

    log.info("search done | reply length={}", reply.length());

    session.reply(reply);
    updateInspectorState(
        session, context, "Search complete — remaining requirements have not been checked", reply);
    session.logEvent("search-completed", Map.of());
  }

  private void updateInspectorState(
      Session session, JarvisAgentContext context, String status, String searchResult) {
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

    if (searchResult != null) {
      state += "\n## Search Result\n\n" + searchResult;
    }

    session.updateState(state);
  }
}
