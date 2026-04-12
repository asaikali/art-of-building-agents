package com.example.jarvis.requirements.alignment;

import com.example.jarvis.requirements.UserRequirements;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class RequirementsExtractor {

  private static final String SYSTEM_PROMPT =
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
      """;

  private static final String USER_PROMPT_TEMPLATE =
      """
      Current planning state:
      %s

      New user message:
      %s

      Return the updated planning state.

      %s
      """;

  private static final BeanOutputConverter<UserRequirements> OUTPUT_CONVERTER =
      new BeanOutputConverter<>(UserRequirements.class);

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private final ChatClient extractionClient;

  public RequirementsExtractor(ChatClient.Builder chatClientBuilder) {
    this.extractionClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
  }

  protected RequirementsExtractor() {
    this.extractionClient = null;
  }

  public UserRequirements extract(UserRequirements currentRequirements, String userMessage) {
    String currentStateJson = serializeToJson(currentRequirements);
    String prompt =
        USER_PROMPT_TEMPLATE.formatted(
            currentStateJson, userMessage.trim(), OUTPUT_CONVERTER.getFormat());
    return extractionClient.prompt().user(prompt).call().entity(OUTPUT_CONVERTER);
  }

  private static String serializeToJson(UserRequirements requirements) {
    try {
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(requirements);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize UserRequirements to JSON", e);
    }
  }
}
