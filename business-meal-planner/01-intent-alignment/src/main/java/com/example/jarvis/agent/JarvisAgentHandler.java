package com.example.jarvis.agent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import com.example.jarvis.requirements.alignment.RequirementsAlignmentLoop;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JarvisAgentHandler implements AgentHandler {

  private final RequirementsAlignmentLoop requirementsAlignmentLoop;

  public JarvisAgentHandler(RequirementsAlignmentLoop requirementsAlignmentLoop) {
    this.requirementsAlignmentLoop = requirementsAlignmentLoop;
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
        Tell me about the occasion, and I’ll help you design something just right.
        """;
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    long turn = session.getMessages().stream().filter(m -> m.role() == Role.USER).count();
    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));

    JarvisAgentContext context = getOrCreateContext(session);
    RequirementsAlignmentLoop.TurnResult result =
        requirementsAlignmentLoop.handleTurn(context, message.text());
    JarvisAgentContext state = result.state();

    session.logEvent(
        result.eventName(),
        Map.of(
            "turn", turn,
            "status", state.getStatus().label(),
            "missingInformationCount", state.getMissingInformation().size()));
    session.updateState(state.toMarkdown());
    session.appendMessage(Role.ASSISTANT, result.assistantReply());
    session.logEvent(
        "assistant-reply-sent", Map.of("turn", turn, "reply", result.assistantReply()));
  }

  private JarvisAgentContext getOrCreateContext(Session session) {
    return session
        .getAgentContext()
        .filter(JarvisAgentContext.class::isInstance)
        .map(JarvisAgentContext.class::cast)
        .orElseGet(
            () -> {
              JarvisAgentContext context = new JarvisAgentContext();
              session.setAgentContext(context);
              return context;
            });
  }
}
