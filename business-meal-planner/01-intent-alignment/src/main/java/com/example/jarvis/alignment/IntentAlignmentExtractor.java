package com.example.jarvis.alignment;

import com.example.jarvis.state.UserGoals;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class IntentAlignmentExtractor {

  private static final String EXTRACTION_SYSTEM_PROMPT =
      """
      You convert business meal requests into a structured planning artifact.

      Rules:
      - Extract only facts the user actually stated into constraints.
      - Populate date when the user gives a date.
      - Populate time when the user gives a time.
      - Populate partySize when the user gives the number of attendees.
      - Keep every list item short and concrete.
      - Capture action intent explicitly when present, such as recommendations only,
        shortlist only, book now, or do not book yet.
      - Do not recommend restaurants.
      - Do not rank or book anything.
      """;

  private static final BeanOutputConverter<ExtractedRequirements> OUTPUT_CONVERTER =
      new BeanOutputConverter<>(ExtractedRequirements.class);

  private final ChatClient extractionClient;

  public IntentAlignmentExtractor(ChatClient.Builder chatClientBuilder) {
    this.extractionClient = chatClientBuilder.defaultSystem(EXTRACTION_SYSTEM_PROMPT).build();
  }

  protected IntentAlignmentExtractor() {
    this.extractionClient = null;
  }

  public UserGoals extractRequirements(UserGoals existingGoals, String userMessage) {
    String prompt =
        existingGoals == null
            ? """
              User request:
              %s

              %s
              """
                .formatted(userMessage.trim(), OUTPUT_CONVERTER.getFormat())
            : """
              Existing user goals JSON:
              %s

              New user message:
              %s

              Update the plan by preserving valid constraints, incorporating corrections and
              additions, and removing anything the new message contradicts.

              %s
              """
                .formatted(toJson(existingGoals), userMessage.trim(), OUTPUT_CONVERTER.getFormat());

    ExtractedRequirements draft =
        extractionClient.prompt().user(prompt).call().entity(OUTPUT_CONVERTER);
    return UserGoals.fromValues(
        draft.intent(), draft.date(), draft.time(), draft.partySize(), draft.constraints());
  }

  private String toJson(UserGoals userGoals) {
    return """
        {
          "intent": "%s",
          "date": %s,
          "time": %s,
          "partySize": %s,
          "constraints": %s
        }
        """
        .formatted(
            escape(userGoals.getIntent()),
            toJsonValue(userGoals.getDate()),
            toJsonValue(userGoals.getTime()),
            userGoals.getPartySize() == null ? "null" : userGoals.getPartySize().toString(),
            toJsonArray(userGoals.getConstraints()));
  }

  private String toJsonValue(Object value) {
    return value == null ? "null" : quote(value.toString());
  }

  private String toJsonArray(java.util.List<String> values) {
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

  private record ExtractedRequirements(
      String intent,
      LocalDate date,
      LocalTime time,
      Integer partySize,
      java.util.List<String> constraints) {}
}
