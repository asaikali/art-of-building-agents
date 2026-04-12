package com.example.jarvis.requirements.alignment;

import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.UserRequirements;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ReplyComposer {

  private final ChatClient chatClient;

  public ReplyComposer(ChatClient.Builder chatClientBuilder) {
    this.chatClient =
        chatClientBuilder
            .defaultSystem(
                """
                You are Jarvis, a warm and professional executive dining assistant. You help
                plan business meals by gathering requirements through natural conversation.

                Your tone should be:
                - Conversational and human, like a real assistant, not a form
                - Brief, typically 2-4 sentences unless summarizing captured requirements
                - Specific, name the actual detail you are asking about
                - Never robotic, do not say things like "I have captured the following fields"

                Follow the instructions precisely. Do not ask about things that are not mentioned.
                Do not skip what the instructions tell you to cover.
                """)
            .build();
  }

  public String composeClarificationReply(
      String missingField, UserRequirements requirements, List<AgentMessage> conversationHistory) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                    We still need some information before we can start looking for restaurants.
                    Write a brief, friendly question asking about the missing field below.
                    Do not list everything that is missing, just ask about the one thing.

                    <missingField>
                    {missingField}
                    </missingField>

                    <requirements>
                    {requirements}
                    </requirements>

                    <conversation>
                    {conversation}
                    </conversation>
                    """)
                    .param("missingField", missingField)
                    .param("requirements", JsonUtils.toJson(requirements))
                    .param("conversation", formatConversation(conversationHistory)))
        .call()
        .content();
  }

  public String composeConfirmationReply(
      String suggestedFollowUp,
      UserRequirements requirements,
      List<AgentMessage> conversationHistory) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                    We have enough information to start searching for restaurants. Before we do,
                    summarize what the user has told us and ask them to confirm or correct anything.

                    <requirements>
                    {requirements}
                    </requirements>

                    <suggestedFollowUp>
                    {suggestedFollowUp}
                    </suggestedFollowUp>

                    <conversation>
                    {conversation}
                    </conversation>

                    Summarize the requirements in a readable way. If the suggested follow-up is
                    relevant, mention it casually. End by asking the user to confirm or correct.
                    """)
                    .param("requirements", JsonUtils.toJson(requirements))
                    .param("suggestedFollowUp", suggestedFollowUp)
                    .param("conversation", formatConversation(conversationHistory)))
        .call()
        .content();
  }

  public String composeConfirmedReply(
      UserRequirements requirements, List<AgentMessage> conversationHistory) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                    The user has confirmed the requirements. Acknowledge briefly and let them
                    know you will start looking for restaurants.

                    <requirements>
                    {requirements}
                    </requirements>

                    <conversation>
                    {conversation}
                    </conversation>

                    Do not repeat all the details. Keep it short.
                    """)
                    .param("requirements", JsonUtils.toJson(requirements))
                    .param("conversation", formatConversation(conversationHistory)))
        .call()
        .content();
  }

  private String formatConversation(List<AgentMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return "(no prior messages)";
    }
    StringBuilder sb = new StringBuilder();
    for (AgentMessage msg : messages) {
      sb.append(msg.role().name()).append(": ").append(msg.text()).append("\n");
    }
    return sb.toString().trim();
  }
}
