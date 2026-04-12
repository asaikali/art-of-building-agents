package com.example.jarvis.agent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.session.Session;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JarvisAgentHandler implements AgentHandler {

  private final RequirementsAligner requirementsAligner;

  public JarvisAgentHandler(RequirementsAligner requirementsAligner) {
    this.requirementsAligner = requirementsAligner;
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
    // Retrieve or initialize the workflow state for this session
    var context = session.getOrCreateContext(JarvisAgentContext.class, JarvisAgentContext::new);

    // Run the alignment pipeline: extract → assess → status → reply
    var result =
        requirementsAligner.processMessage(
            context.getUserRequirements(), context.getStatus(), message.text());

    // Update workflow state with the computed outputs
    context.setUserRequirements(result.updatedRequirements());
    context.setMissingInformation(result.missingRequiredFields());
    context.setStatus(result.status());

    // Send the reply
    session.reply(result.reply());

    // Update inspector state and log the outcome
    session.updateState(context.toMarkdown());
    session.logEvent(
        context.getStatus().eventName(),
        Map.of(
            "status", context.getStatus().label(),
            "missingInformationCount", context.getMissingInformation().size()));
  }
}
