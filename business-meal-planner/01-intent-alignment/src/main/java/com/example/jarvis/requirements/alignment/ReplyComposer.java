package com.example.jarvis.requirements.alignment;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.UserRequirements;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Composes natural-language replies using the model. This is step 4 of the alignment pipeline in
 * {@link RequirementsAligner}.
 *
 * <p>The aligner decides which kind of reply is needed based on the {@link AlignmentStatus}, then
 * calls the corresponding method here:
 *
 * <ul>
 *   <li>{@link #askForMissingField} — asks about a required field that is still missing
 *   <li>{@link #askForConfirmation} — summarizes the requirements and asks the user to confirm
 *   <li>{@link #acknowledgeConfirmation} — acknowledges that the user confirmed
 * </ul>
 *
 * <p>Each method receives the data it needs and the model writes the reply. The system prompt
 * establishes the assistant's tone so each method only needs to describe what to say, not how.
 */
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
                """)
            .build();
  }

  public String askForMissingField(String missingField, UserRequirements requirements) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                    We need to know the following before we can start looking for restaurants.
                    Write a brief, friendly question asking about it.

                    <missingField>
                    {missingField}
                    </missingField>

                    <requirements>
                    {requirements}
                    </requirements>
                    """)
                    .param("missingField", missingField)
                    .param("requirements", JsonUtils.toJson(requirements)))
        .call()
        .content();
  }

  public String askForConfirmation(String suggestedFollowUp, UserRequirements requirements) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                    We have enough information to start searching for restaurants. Summarize the
                    requirements below in a readable way and ask the user to confirm or correct.
                    If the suggested follow-up is relevant, mention it casually.

                    <requirements>
                    {requirements}
                    </requirements>

                    <suggestedFollowUp>
                    {suggestedFollowUp}
                    </suggestedFollowUp>
                    """)
                    .param("requirements", JsonUtils.toJson(requirements))
                    .param("suggestedFollowUp", suggestedFollowUp))
        .call()
        .content();
  }

  public String acknowledgeConfirmation(UserRequirements requirements) {
    return chatClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                    The user has confirmed the requirements below. Acknowledge briefly and let
                    them know you will start looking for restaurants.

                    <requirements>
                    {requirements}
                    </requirements>
                    """)
                    .param("requirements", JsonUtils.toJson(requirements)))
        .call()
        .content();
  }
}
