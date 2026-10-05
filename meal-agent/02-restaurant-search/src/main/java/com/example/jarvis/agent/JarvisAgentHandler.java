package com.example.jarvis.agent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.session.Session;
import com.example.jarvis.requirements.alignment.AlignmentStatus;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import com.example.jarvis.search.RestaurantSearcher;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JarvisAgentHandler implements AgentHandler {

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

    session.reply(result.reply());
    session.updateState(InspectorState.render(context, context.getAlignmentStatus().label(), null));
    session.logEvent(
        context.getAlignmentStatus().label(),
        Map.of("missingFieldCount", result.missingRequiredFields().size()));
  }

  private void handleSearch(Session session, JarvisAgentContext context) {
    session.logEvent("search-started", Map.of());
    session.updateState(
        InspectorState.render(context, "Searching for available restaurants...", null));

    String reply = restaurantSearcher.search(context.getUserRequirements());

    // Reset to gathering so the next message goes through alignment
    // (user might want to relax constraints and try again)
    context.setAlignmentStatus(AlignmentStatus.GATHERING_REQUIREMENTS);

    session.reply(reply);
    session.updateState(
        InspectorState.render(
            context, "Search complete — remaining requirements have not been checked", reply));
    session.logEvent("search-completed", Map.of());
  }
}
