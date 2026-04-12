package com.example.jarvis.agent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.json.JsonUtils;
import com.example.agent.core.session.Session;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JarvisAgentHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(JarvisAgentHandler.class);

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

    log.info(
        "onMessage | status={} | user=\"{}\"",
        context.getAlignmentStatus().label(),
        message.text());

    // Run the alignment pipeline: extract → determine status → compose reply
    var result =
        requirementsAligner.processMessage(
            context.getUserRequirements(), context.getAlignmentStatus(), message.text());

    // Update workflow state with the computed outputs
    context.setUserRequirements(result.updatedRequirements());
    context.setAlignmentStatus(result.updatedStatus());

    log.info(
        "done | status={} | missingFields={}",
        result.updatedStatus().label(),
        result.missingRequiredFields().size());

    // Send the reply
    session.reply(result.reply());

    // Update inspector state and log the outcome
    session.updateState(
        """
        # Agent Context

        ## Requirements
        ```json
        %s
        ```

        ## Status
        %s
        """
            .formatted(
                JsonUtils.toJson(context.getUserRequirements()),
                context.getAlignmentStatus().label()));
    session.logEvent(
        context.getAlignmentStatus().label(),
        Map.of("missingFieldCount", result.missingRequiredFields().size()));
  }
}
