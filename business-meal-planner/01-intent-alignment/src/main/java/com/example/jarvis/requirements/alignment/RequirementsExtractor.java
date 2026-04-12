package com.example.jarvis.requirements.alignment;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.UserRequirements;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Extracts structured {@link UserRequirements} from a user message using the model. This is step 1
 * of the alignment pipeline in {@link RequirementsAligner}.
 *
 * <p>Each call takes the current requirements and a new user message, and returns an updated {@link
 * UserRequirements} with the new information merged in. The model preserves existing data unless
 * the user explicitly corrects it, so requirements accumulate over multiple turns.
 */
@Component
public class RequirementsExtractor {

  private static final Logger log = LoggerFactory.getLogger(RequirementsExtractor.class);

  private final ChatClient extractionClient;

  public RequirementsExtractor(ChatClient.Builder chatClientBuilder) {
    this.extractionClient =
        chatClientBuilder
            .defaultSystem(
                """
                You are a structured data extractor for a business meal planning assistant.

                Your job: given the current planning state and a new user message, return an
                updated UserRequirements JSON that merges the new information.

                Modeling rules:
                - Meal holds shared facts about the meal: date, time, party size, type, purpose,
                  budget, noise level, cuisine preferences, and additional requirements.
                - Attendees hold per-person details: name, origin, travel, and dietary constraints.
                - If something varies by person (e.g. dietary needs, origin), put it on an attendee.
                - If something applies to the whole meal (e.g. budget, noise level), put it on meal.

                Extraction rules:
                - Preserve all existing valid information unless the new message explicitly corrects it.
                - Add new information from the user message.
                - Remove information only when the user explicitly contradicts it.
                - If the user message is a greeting, affirmation, or contains no plannable information,
                  return the current state unchanged.
                - Do not infer information the user did not provide. If they say "dinner" but no time,
                  set mealType to DINNER but leave time null.
                - Use null for unknown fields. Never use empty strings for enum or date/time fields.
                - Resolve relative dates like "tomorrow" or "next Tuesday" using today's date
                  provided in the user prompt.
                - Use dedicated fields before additionalRequirements. For example, dietary needs go
                  on an attendee's dietaryConstraints, not in additionalRequirements.
                """)
            .build();
  }

  public UserRequirements extract(UserRequirements currentRequirements, String userMessage) {
    log.info(
        "input | currentRequirements={} | userMessage=\"{}\"",
        JsonUtils.toJson(currentRequirements),
        userMessage.trim());

    UserRequirements result =
        extractionClient
            .prompt()
            .user(
                u ->
                    u.text(
                            """
                    Today's date: {today}

                    <currentState>
                    {currentState}
                    </currentState>

                    <userMessage>
                    {userMessage}
                    </userMessage>

                    Return the updated planning state.
                    """)
                        .param("today", LocalDate.now().toString())
                        .param("currentState", JsonUtils.toJson(currentRequirements))
                        .param("userMessage", userMessage.trim()))
            .call()
            .entity(UserRequirements.class);

    log.info("output | extractedRequirements={}", JsonUtils.toJson(result));
    return result;
  }
}
