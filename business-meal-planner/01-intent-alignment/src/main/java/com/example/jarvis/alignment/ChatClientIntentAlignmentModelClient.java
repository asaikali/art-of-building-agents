package com.example.jarvis.alignment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ChatClientIntentAlignmentModelClient implements IntentAlignmentModelClient {

  private static final Pattern PEOPLE_PATTERN =
      Pattern.compile("\\b(\\d+)\\s+(people|person|guests|guest)\\b", Pattern.CASE_INSENSITIVE);

  private static final Pattern DATE_TIME_PATTERN =
      Pattern.compile(
          "\\b(today|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday|next\\s+\\w+)\\b.*?\\b(\\d{1,2}(?::\\d{2})?\\s*(am|pm))\\b",
          Pattern.CASE_INSENSITIVE);

  private static final String EXTRACTION_SYSTEM_PROMPT =
      """
      You convert business meal requests into a structured planning artifact.

      Return JSON only. Do not wrap the JSON in markdown unless the model absolutely insists.
      Use this exact object shape:
      {
        "intent": "string",
        "explicitConstraints": ["string"],
        "inferredConstraints": ["string"],
        "missingInformation": ["string"],
        "assumptions": ["string"]
      }

      Rules:
      - Extract only facts the user actually stated into explicitConstraints.
      - Always capture action intent explicitly when present, such as recommend, shortlist, book,
        do not book yet, or recommendations only.
      - Add reasonable business-meal defaults into inferredConstraints.
      - Put unresolved but important facts into missingInformation.
      - Put tentative interpretations into assumptions.
      - Keep every list item short and concrete.
      - Do not recommend restaurants.
      - Do not rank or book anything.
      - Do not add extra keys.
      """;

  private static final String SUMMARY_SYSTEM_PROMPT =
      """
      You are summarizing a business meal planning artifact back to the user.

      Rules:
      - Be concise and clear.
      - Reflect the plan accurately.
      - If status is "Waiting for confirmation", summarize the understanding and ask the user to
        confirm or correct it.
      - If status is "Waiting for clarification", ask one targeted clarification question focused on
        the single most important missing detail.
      - If status is "Requirements confirmed", state that the requirements are confirmed and that
        phase 1 is complete.
      - Do not search for restaurants.
      - Do not mention later phases.
      """;

  private static final String JSON_RECOVERY_SYSTEM_PROMPT =
      """
      You repair business meal planning output into valid JSON.

      Return JSON only. Do not include explanations.
      Use this exact object shape:
      {
        "intent": "string",
        "explicitConstraints": ["string"],
        "inferredConstraints": ["string"],
        "missingInformation": ["string"],
        "assumptions": ["string"]
      }

      If the source text is a user-facing summary, clarification question, or partial plan,
      infer the best possible JSON representation from that text.
      Preserve action intent when the source says things like recommend, shortlist, book,
      don't book yet, or recommendations only.
      Do not add extra keys.
      """;

  private final ChatClient extractionClient;
  private final ChatClient jsonRecoveryClient;
  private final ChatClient summaryClient;
  private final ObjectMapper objectMapper;
  private final IntentAlignmentMarkdownRenderer markdownRenderer;

  public ChatClientIntentAlignmentModelClient(
      ChatClient.Builder chatClientBuilder, IntentAlignmentMarkdownRenderer markdownRenderer) {
    this.extractionClient = chatClientBuilder.defaultSystem(EXTRACTION_SYSTEM_PROMPT).build();
    this.jsonRecoveryClient = chatClientBuilder.defaultSystem(JSON_RECOVERY_SYSTEM_PROMPT).build();
    this.summaryClient = chatClientBuilder.defaultSystem(SUMMARY_SYSTEM_PROMPT).build();
    this.objectMapper = new ObjectMapper();
    this.markdownRenderer = markdownRenderer;
  }

  @Override
  public BusinessMealRequirements createInitialPlan(String userMessage, RequirementStatus status) {
    String prompt =
        """
        User request:
        %s

        Produce the JSON object now.
        """
            .formatted(userMessage.trim());
    return overlayDirectSignal(extractPlan(prompt, status), userMessage);
  }

  @Override
  public BusinessMealRequirements revisePlan(
      BusinessMealRequirements existingRequirements, String userMessage, RequirementStatus status) {
    String prompt =
        """
        Existing plan JSON:
        %s

        New user message:
        %s

        Update the plan by preserving valid constraints, incorporating corrections and additions,
        and removing anything the new message contradicts.
        Pay special attention to action intent such as recommend only, shortlist only, book now,
        or do not book yet.

        Produce the JSON object now.
        """
            .formatted(toJson(existingRequirements), userMessage.trim());
    return overlayDirectSignal(extractPlan(prompt, status), userMessage);
  }

  @Override
  public String summarizePlan(BusinessMealRequirements requirements) {
    String prompt =
        """
        Planning artifact:
        %s

        Write the user-facing response now.
        """
            .formatted(markdownRenderer.render(requirements));
    return summaryClient.prompt().user(prompt).call().content().trim();
  }

  private BusinessMealRequirements extractPlan(String prompt, RequirementStatus status) {
    String raw = extractionClient.prompt().user(prompt).call().content();
    return parsePlan(raw, status);
  }

  private BusinessMealRequirements parsePlan(String raw, RequirementStatus status) {
    try {
      return BusinessMealRequirements.fromDraft(parseDraft(raw), status);
    } catch (JsonProcessingException e) {
      BusinessMealRequirementsDraft fallbackFromRaw = tryFlexibleJson(raw);
      if (fallbackFromRaw != null) {
        return BusinessMealRequirements.fromDraft(fallbackFromRaw, status);
      }
      BusinessMealRequirementsDraft textFallbackFromRaw = tryTextHeuristics(raw);
      if (textFallbackFromRaw != null) {
        return BusinessMealRequirements.fromDraft(textFallbackFromRaw, status);
      }

      String recovered = recoverJson(raw);
      try {
        return BusinessMealRequirements.fromDraft(parseDraft(recovered), status);
      } catch (JsonProcessingException recoveryFailure) {
        BusinessMealRequirementsDraft fallbackFromRecovered = tryFlexibleJson(recovered);
        if (fallbackFromRecovered != null) {
          return BusinessMealRequirements.fromDraft(fallbackFromRecovered, status);
        }
        BusinessMealRequirementsDraft textFallbackFromRecovered = tryTextHeuristics(recovered);
        if (textFallbackFromRecovered != null) {
          return BusinessMealRequirements.fromDraft(textFallbackFromRecovered, status);
        }

        throw new IllegalStateException(
            "Failed to parse intent-alignment model output. Raw output: "
                + raw
                + " Recovered output: "
                + recovered,
            recoveryFailure);
      }
    }
  }

  private BusinessMealRequirementsDraft tryTextHeuristics(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }

    String text = stripCodeFences(raw).trim();
    String lower = text.toLowerCase();

    LinkedHashSet<String> explicitConstraints = new LinkedHashSet<>();
    LinkedHashSet<String> inferredConstraints = new LinkedHashSet<>();
    LinkedHashSet<String> missingInformation = new LinkedHashSet<>();
    LinkedHashSet<String> assumptions = new LinkedHashSet<>();

    String intent = inferIntent(lower);
    if (intent == null) {
      intent = "Clarify the business meal request.";
    }

    Matcher dateTimeMatcher = DATE_TIME_PATTERN.matcher(text);
    if (dateTimeMatcher.find()) {
      explicitConstraints.add(dateTimeMatcher.group().trim());
    }

    Matcher peopleMatcher = PEOPLE_PATTERN.matcher(text);
    if (peopleMatcher.find()) {
      explicitConstraints.add(peopleMatcher.group().trim());
    }

    if (lower.contains("vegetarian")) {
      explicitConstraints.add("Vegetarian requirement");
    }
    if (lower.contains("vegan")) {
      explicitConstraints.add("Vegan requirement");
    }
    if (lower.contains("gluten")) {
      explicitConstraints.add("Gluten-free requirement");
    }
    if (lower.contains("quiet")) {
      explicitConstraints.add("Quiet enough for conversation");
    }
    if (lower.contains("professional")) {
      explicitConstraints.add("Professional atmosphere");
    }
    if (lower.contains("union station")) {
      explicitConstraints.add("Near Union Station");
    }
    if (lower.contains("don't book") || lower.contains("do not book")) {
      explicitConstraints.add("Do not book yet");
    }
    if (lower.contains("recommendation") || lower.contains("recommend")) {
      explicitConstraints.add("Recommendations only");
    }

    if (lower.contains("client")) {
      inferredConstraints.add("Venue should be suitable for client conversation");
    }

    if (lower.contains("date and time")) {
      missingInformation.add("Date and time");
    }
    if (lower.contains("number of attendees") || lower.contains("party size")) {
      missingInformation.add("Party size");
    }
    if (lower.contains("location preference")) {
      missingInformation.add("Location preference");
    }
    if (lower.contains("budget range")) {
      missingInformation.add("Budget range");
    }
    if (lower.contains("preferred cuisine") || lower.contains("cuisine preference")) {
      missingInformation.add("Cuisine preference");
    }

    if (text.contains("?") && missingInformation.isEmpty() && explicitConstraints.isEmpty()) {
      missingInformation.add("Clarifying details");
    }

    if (explicitConstraints.isEmpty()
        && inferredConstraints.isEmpty()
        && missingInformation.isEmpty()
        && assumptions.isEmpty()) {
      return null;
    }

    return new BusinessMealRequirementsDraft(
        intent,
        new ArrayList<>(explicitConstraints),
        new ArrayList<>(inferredConstraints),
        new ArrayList<>(missingInformation),
        new ArrayList<>(assumptions));
  }

  private BusinessMealRequirementsDraft parseDraft(String raw) throws JsonProcessingException {
    return objectMapper.readValue(stripCodeFences(raw), BusinessMealRequirementsDraft.class);
  }

  private BusinessMealRequirementsDraft tryFlexibleJson(String raw) {
    String json = extractFirstJsonObject(raw);
    if (json == null) {
      return null;
    }

    try {
      JsonNode root = objectMapper.readTree(json);
      return mapFlexibleNode(root);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private BusinessMealRequirementsDraft mapFlexibleNode(JsonNode root) {
    String intent = firstText(root, "intent", "Intent", "event", "Event", "mealType", "MealType");
    if (intent == null || intent.isBlank()) {
      intent = "Clarify the business meal request.";
    }

    LinkedHashSet<String> explicitConstraints = new LinkedHashSet<>();
    LinkedHashSet<String> inferredConstraints = new LinkedHashSet<>();
    LinkedHashSet<String> missingInformation = new LinkedHashSet<>();
    LinkedHashSet<String> assumptions = new LinkedHashSet<>();

    addAll(explicitConstraints, readStringValues(root, "explicitConstraints"));
    addAll(inferredConstraints, readStringValues(root, "inferredConstraints"));
    addAll(missingInformation, readStringValues(root, "missingInformation"));
    addAll(assumptions, readStringValues(root, "assumptions"));

    addKnownField(explicitConstraints, root, "DateAndTime", "Date and Time");
    addKnownField(explicitConstraints, root, "dateTime", "Date and Time");
    addKnownField(explicitConstraints, root, "date", "Date");
    addKnownField(explicitConstraints, root, "time", "Time");
    addKnownField(explicitConstraints, root, "PartySize", "Party Size");
    addKnownField(explicitConstraints, root, "partySize", "Party Size");
    addKnownField(explicitConstraints, root, "DietaryConsideration", "Dietary Consideration");
    addKnownField(explicitConstraints, root, "dietaryConsideration", "Dietary Consideration");
    addKnownField(explicitConstraints, root, "Atmosphere", "Atmosphere");
    addKnownField(explicitConstraints, root, "atmosphere", "Atmosphere");
    addKnownField(explicitConstraints, root, "locationPreference", "Location Preference");
    addKnownField(explicitConstraints, root, "budgetRange", "Budget Range");

    JsonNode details = root.path("details");
    if (details.isObject()) {
      addDetail(explicitConstraints, missingInformation, details, "date", "Date");
      addDetail(explicitConstraints, missingInformation, details, "time", "Time");
      addDetail(
          explicitConstraints,
          missingInformation,
          details,
          "numberOfAttendees",
          "Number of attendees");
      addDetail(
          explicitConstraints,
          missingInformation,
          details,
          "cuisinePreference",
          "Cuisine preference");
      addDetail(explicitConstraints, missingInformation, details, "budgetRange", "Budget range");
      addDetail(
          explicitConstraints,
          missingInformation,
          details,
          "locationPreference",
          "Location preference");
    }

    return new BusinessMealRequirementsDraft(
        intent,
        new ArrayList<>(explicitConstraints),
        new ArrayList<>(inferredConstraints),
        new ArrayList<>(missingInformation),
        new ArrayList<>(assumptions));
  }

  private String recoverJson(String raw) {
    String prompt =
        """
        Convert the following output into the required JSON object.

        Source output:
        %s
        """
            .formatted(raw);
    return jsonRecoveryClient.prompt().user(prompt).call().content();
  }

  private String toJson(BusinessMealRequirements requirements) {
    try {
      return objectMapper.writeValueAsString(requirements);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize intent-alignment plan", e);
    }
  }

  private String stripCodeFences(String raw) {
    String trimmed = raw == null ? "" : raw.trim();
    if (!trimmed.startsWith("```")) {
      return trimmed;
    }

    int firstNewline = trimmed.indexOf('\n');
    int lastFence = trimmed.lastIndexOf("```");
    if (firstNewline < 0 || lastFence <= firstNewline) {
      return trimmed;
    }
    return trimmed.substring(firstNewline + 1, lastFence).trim();
  }

  private String extractFirstJsonObject(String raw) {
    String text = stripCodeFences(raw);
    int start = text.indexOf('{');
    if (start < 0) {
      return null;
    }

    int depth = 0;
    for (int i = start; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return text.substring(start, i + 1);
        }
      }
    }
    return null;
  }

  private String firstText(JsonNode node, String... fieldNames) {
    for (String fieldName : fieldNames) {
      JsonNode child = node.path(fieldName);
      if (child.isTextual() && !child.asText().isBlank()) {
        return child.asText().trim();
      }
    }
    return null;
  }

  private List<String> readStringValues(JsonNode node, String fieldName) {
    JsonNode child = node.path(fieldName);
    if (child.isMissingNode() || child.isNull()) {
      return List.of();
    }
    if (child.isArray()) {
      List<String> values = new ArrayList<>();
      for (JsonNode item : child) {
        if (item.isTextual() && !item.asText().isBlank()) {
          values.add(item.asText().trim());
        }
      }
      return values;
    }
    if (child.isTextual() && !child.asText().isBlank()) {
      return List.of(child.asText().trim());
    }
    return List.of();
  }

  private void addAll(LinkedHashSet<String> target, List<String> values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        target.add(value.trim());
      }
    }
  }

  private void addKnownField(
      LinkedHashSet<String> target, JsonNode node, String field, String label) {
    String value = firstText(node, field);
    if (value != null) {
      target.add(label + ": " + value);
    }
  }

  private void addDetail(
      LinkedHashSet<String> explicitConstraints,
      LinkedHashSet<String> missingInformation,
      JsonNode details,
      String fieldName,
      String label) {
    JsonNode value = details.path(fieldName);
    if (value.isMissingNode() || value.isNull()) {
      missingInformation.add(label);
      return;
    }
    if (value.isTextual() && !value.asText().isBlank()) {
      explicitConstraints.add(label + ": " + value.asText().trim());
    } else if (value.isNumber() || value.isBoolean()) {
      explicitConstraints.add(label + ": " + value.asText());
    }
  }

  private String inferIntent(String lower) {
    if (lower.contains("dinner")) {
      return "Plan a business dinner.";
    }
    if (lower.contains("lunch")) {
      return "Plan a business lunch.";
    }
    if (lower.contains("meal")) {
      return "Plan a business meal.";
    }
    return null;
  }

  private BusinessMealRequirements overlayDirectSignal(
      BusinessMealRequirements requirements, String userMessage) {
    BusinessMealRequirementsDraft directSignal = tryTextHeuristics(userMessage);
    if (directSignal == null) {
      return requirements;
    }

    String intent = requirements.intent();
    if (shouldPreferDirectIntent(intent, directSignal.intent())) {
      intent = directSignal.intent();
    }

    return new BusinessMealRequirements(
        intent,
        merge(requirements.explicitConstraints(), directSignal.explicitConstraints()),
        merge(requirements.inferredConstraints(), directSignal.inferredConstraints()),
        merge(requirements.missingInformation(), directSignal.missingInformation()),
        merge(requirements.assumptions(), directSignal.assumptions()),
        requirements.status());
  }

  private List<String> merge(List<String> primary, List<String> overlay) {
    LinkedHashSet<String> merged = new LinkedHashSet<>();
    merged.addAll(primary);
    merged.addAll(overlay);
    return new ArrayList<>(merged);
  }

  private boolean shouldPreferDirectIntent(String currentIntent, String directIntent) {
    if (directIntent == null || directIntent.isBlank()) {
      return false;
    }
    if (currentIntent == null || currentIntent.isBlank()) {
      return true;
    }
    String current = currentIntent.toLowerCase();
    String direct = directIntent.toLowerCase();
    if ("clarify the business meal request.".equals(current)) {
      return true;
    }
    if ("plan a business meal.".equals(current)
        && ("plan a business lunch.".equals(direct) || "plan a business dinner.".equals(direct))) {
      return true;
    }
    return false;
  }
}
