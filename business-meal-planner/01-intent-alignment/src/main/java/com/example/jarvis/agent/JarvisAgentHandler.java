package com.example.jarvis.agent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
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
        Tell me about the occasion, and I’ll help you design something just right.
        """;
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    long turn = session.getMessages().stream().filter(m -> m.role() == Role.USER).count();
    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));

    JarvisAgentContext context =
        session.getOrCreateContext(JarvisAgentContext.class, JarvisAgentContext::new);
    RequirementsAligner.Result result =
        requirementsAligner.processMessage(context, message.text(), session.getMessages());
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
}
