package com.example.jarvis.requirements.alignment;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.UserRequirements;
import java.time.LocalDate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class RequirementsExtractor {

  private final ChatClient extractionClient;

  public RequirementsExtractor(ChatClient.Builder chatClientBuilder) {
    this.extractionClient =
        chatClientBuilder
            .defaultSystem(
                """
                You are a structured data extractor for a business meal planning assistant.

                Your job: given the current planning state and a new user message, return an
                updated UserRequirements JSON that merges the new information.

                Rules:
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
                - Do not recommend restaurants, rank options, or make booking decisions.
                - Keep additionalRequirements and cuisinePreferences short and concrete.

                Domain model:
                - Meal: date (ISO LocalDate e.g. 2026-04-13), time (ISO LocalTime e.g. 18:00),
                  partySize (integer), mealType (BREAKFAST | BRUNCH | LUNCH | DINNER),
                  purpose (string), budgetPerPerson (decimal),
                  noiseLevel (QUIET | MODERATE | LIVELY),
                  additionalRequirements (list of strings), cuisinePreferences (list of strings)
                - Attendee: name (string), origin (string), departureTime (ISO LocalTime),
                  travelMode (WALKING | DRIVING | TRANSIT | TAXI),
                  maxTravelTimeMinutes (integer), maxDistanceKm (double),
                  dietaryConstraints (list of VEGETARIAN | VEGAN | GLUTEN_FREE | HALAL | KOSHER | NONE | OTHER)
                """)
            .build();
  }

  public UserRequirements extract(UserRequirements currentRequirements, String userMessage) {
    return extractionClient
        .prompt()
        .user(
            u ->
                u.text(
                        """
                    Today's date: {today}

                    Current planning state:
                    {currentState}

                    New user message:
                    {userMessage}

                    Return the updated planning state.
                    """)
                    .param("today", LocalDate.now().toString())
                    .param("currentState", JsonUtils.toJson(currentRequirements))
                    .param("userMessage", userMessage.trim()))
        .call()
        .entity(UserRequirements.class);
  }
}
