package com.example.jarvis.requirements.alignment;

import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.requirements.Attendee;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.UserRequirements;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class RequirementsExtractor {

  private static final String EXTRACTION_SYSTEM_PROMPT =
      """
      You convert business meal requests into a structured planning context.

      Capture only user-provided inputs.
      Do not infer search areas, validation results, readiness flags, or planning logic.

      Modeling rules:
      - Meal describes the shared facts about the meal as a whole.
      - Attendees describe person-specific needs.
      - If something varies by person, put it on an attendee.
      - If something applies to the whole meal, put it on meal.
      - Preserve valid existing information unless the new message corrects it.
      - Keep additionalRequirements and cuisinePreferences short and concrete.
      - Do not recommend restaurants.
      - Do not rank or book anything.
      """;

  private static final BeanOutputConverter<UserRequirements> OUTPUT_CONVERTER =
      new BeanOutputConverter<>(UserRequirements.class);

  private final ChatClient extractionClient;

  public RequirementsExtractor(ChatClient.Builder chatClientBuilder) {
    this.extractionClient = chatClientBuilder.defaultSystem(EXTRACTION_SYSTEM_PROMPT).build();
  }

  protected RequirementsExtractor() {
    this.extractionClient = null;
  }

  public UserRequirements extract(JarvisAgentContext existingState, String userMessage) {
    String prompt =
        existingState == null || existingState.getUserRequirements().isEmpty()
            ? """
              User request:
              %s

              %s
              """
                .formatted(userMessage.trim(), OUTPUT_CONVERTER.getFormat())
            : """
              Existing planning context JSON:
              %s

              New user message:
              %s

              Update the planning context by preserving valid details, incorporating
              corrections and additions, and removing anything the new message contradicts.

              %s
              """
                .formatted(toJson(existingState), userMessage.trim(), OUTPUT_CONVERTER.getFormat());

    return extractionClient.prompt().user(prompt).call().entity(OUTPUT_CONVERTER);
  }

  private String toJson(JarvisAgentContext state) {
    UserRequirements userRequirements = state.getUserRequirements();
    Meal meal = userRequirements.getMeal();
    return """
        {
          "meal": {
            "date": %s,
            "time": %s,
            "partySize": %s,
            "mealType": %s,
            "purpose": %s,
            "budgetPerPerson": %s,
            "noiseLevel": %s,
            "additionalRequirements": %s,
            "cuisinePreferences": %s
          },
          "attendees": %s
        }
        """
        .formatted(
            toJsonValue(meal.getDate()),
            toJsonValue(meal.getTime()),
            meal.getPartySize() == null ? "null" : meal.getPartySize().toString(),
            toJsonValue(meal.getMealType()),
            toJsonValue(meal.getPurpose()),
            meal.getBudgetPerPerson() == null ? "null" : meal.getBudgetPerPerson().toPlainString(),
            toJsonValue(meal.getNoiseLevel()),
            toJsonArray(meal.getAdditionalRequirements()),
            toJsonArray(meal.getCuisinePreferences()),
            toAttendeeJsonArray(userRequirements.getAttendees()));
  }

  private String toAttendeeJsonArray(List<Attendee> attendees) {
    return attendees.stream()
        .map(
            attendee ->
                """
                {
                  "name": %s,
                  "origin": %s,
                  "departureTime": %s,
                  "travelMode": %s,
                  "maxTravelTimeMinutes": %s,
                  "maxDistanceKm": %s,
                  "dietaryConstraints": %s
                }
                """
                    .formatted(
                        toJsonValue(attendee.getName()),
                        toJsonValue(attendee.getOrigin()),
                        toJsonValue(attendee.getDepartureTime()),
                        toJsonValue(attendee.getTravelMode()),
                        attendee.getMaxTravelTimeMinutes() == null
                            ? "null"
                            : attendee.getMaxTravelTimeMinutes().toString(),
                        attendee.getMaxDistanceKm() == null
                            ? "null"
                            : attendee.getMaxDistanceKm().toString(),
                        toJsonArray(
                            attendee.getDietaryConstraints().stream().map(Enum::name).toList())))
        .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
  }

  private String toJsonValue(Object value) {
    return value == null ? "null" : quote(value.toString());
  }

  private String toJsonArray(List<String> values) {
    return values.stream()
        .map(this::quote)
        .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
  }

  private String quote(String value) {
    return "\"" + escape(value) + "\"";
  }

  private String escape(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
