package com.example.jarvis.requirements.alignment;

import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.json.JsonUtils;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class RequirementsReplyWriter {

  private static final String SYSTEM_PROMPT =
      """
      You are Jarvis, a warm and professional executive dining assistant. You help
      plan business meals by gathering requirements through natural conversation.

      Write your reply based on the directive you receive. Your tone should be:
      - Conversational and human, like a real assistant, not a form
      - Brief, typically 2-4 sentences unless summarizing captured requirements
      - Specific, name the actual detail you are asking about
      - Never robotic, do not say things like "I have captured the following fields"

      You will receive a directive telling you what kind of reply to write, the current
      captured requirements, and recent conversation for context.

      Follow the directive precisely. Do not ask about things the directive does not mention.
      Do not skip what the directive tells you to cover.
      """;

  private static final int RECENT_MESSAGE_LIMIT = 6;

  private final ChatClient replyClient;

  public RequirementsReplyWriter(ChatClient.Builder chatClientBuilder) {
    this.replyClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
  }

  public String writeReply(ReplyDirective directive, List<AgentMessage> conversationHistory) {
    String prompt = buildPrompt(directive, conversationHistory);
    return replyClient.prompt().user(prompt).call().content();
  }

  private String buildPrompt(ReplyDirective directive, List<AgentMessage> conversationHistory) {
    String requirementsJson = JsonUtils.toJson(directive.currentRequirements());
    String recentConversation = formatRecentMessages(conversationHistory);

    return switch (directive.status()) {
      case WAITING_FOR_CLARIFICATION ->
          buildClarificationPrompt(directive, requirementsJson, recentConversation);
      case WAITING_FOR_CONFIRMATION ->
          buildConfirmationPrompt(directive, requirementsJson, recentConversation);
      case REQUIREMENTS_CONFIRMED -> buildConfirmedPrompt(requirementsJson, recentConversation);
    };
  }

  private String buildClarificationPrompt(
      ReplyDirective directive, String requirementsJson, String recentConversation) {
    String nextField =
        directive.missingCriticalFields().isEmpty()
            ? "any missing details"
            : directive.missingCriticalFields().getFirst();

    return """
        Directive: Ask the user about %s.
        Current requirements:
        %s

        Recent conversation:
        %s

        Write a brief, friendly question asking specifically about the missing information.
        Do not list everything that is missing, just ask about the one thing.
        """
        .formatted(nextField, requirementsJson, recentConversation);
  }

  private String buildConfirmationPrompt(
      ReplyDirective directive, String requirementsJson, String recentConversation) {
    String suggestions =
        directive.suggestedFollowUps().isEmpty()
            ? "none"
            : String.join(", ", directive.suggestedFollowUps());

    return """
        Directive: Summarize the current requirements and ask for confirmation.
        Current requirements:
        %s

        Suggested optional follow-ups to weave in naturally: %s
        Recent conversation:
        %s

        Summarize what you have captured so far in a readable way. If there are suggested
        follow-ups, mention them casually. End by asking the user to confirm or correct anything.
        """
        .formatted(requirementsJson, suggestions, recentConversation);
  }

  private String buildConfirmedPrompt(String requirementsJson, String recentConversation) {
    return """
        Directive: Acknowledge that the requirements are confirmed.
        Current requirements:
        %s

        Recent conversation:
        %s

        Write a brief acknowledgment that the requirements are confirmed.
        Do not repeat all the details.
        """
        .formatted(requirementsJson, recentConversation);
  }

  private String formatRecentMessages(List<AgentMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return "(no prior messages)";
    }
    int start = Math.max(0, messages.size() - RECENT_MESSAGE_LIMIT);
    List<AgentMessage> recent = messages.subList(start, messages.size());
    StringBuilder sb = new StringBuilder();
    for (AgentMessage msg : recent) {
      sb.append(msg.role().name()).append(": ").append(msg.text()).append("\n");
    }
    return sb.toString().trim();
  }
}
