package com.example.jarvis;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import com.example.jarvis.alignment.BusinessMealRequirements;
import com.example.jarvis.alignment.IntentAlignmentConversationService;
import com.example.jarvis.alignment.IntentAlignmentMarkdownRenderer;
import com.example.jarvis.alignment.IntentAlignmentTurnResult;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class JarvisHandler implements AgentHandler {

  private final IntentAlignmentConversationService conversationService;
  private final IntentAlignmentMarkdownRenderer markdownRenderer;
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public JarvisHandler(
      IntentAlignmentConversationService conversationService,
      IntentAlignmentMarkdownRenderer markdownRenderer) {
    this.conversationService = conversationService;
    this.markdownRenderer = markdownRenderer;
  }

  @Override
  public String getName() {
    return "Jarvis";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();
    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));

    IntentAlignmentTurnResult result = conversationService.handleTurn(session.id(), message.text());
    BusinessMealRequirements requirements = result.requirements();

    session.logEvent(
        result.action().eventName(),
        Map.of(
            "turn", turn,
            "status", requirements.status().label(),
            "missingInformationCount", requirements.missingInformation().size()));
    session.updateState(markdownRenderer.render(requirements));
    session.appendMessage(Role.ASSISTANT, result.assistantReply());
    session.logEvent(
        "assistant-reply-sent", Map.of("turn", turn, "reply", result.assistantReply()));
  }
}
