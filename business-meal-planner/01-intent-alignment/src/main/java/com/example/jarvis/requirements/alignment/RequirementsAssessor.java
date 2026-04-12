package com.example.jarvis.requirements.alignment;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.Meal;
import com.example.jarvis.requirements.UserRequirements;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Assesses {@link UserRequirements} against hard and soft criteria. This is step 2 of the alignment
 * pipeline in {@link RequirementsAligner}.
 *
 * <p>Hard criteria ({@link #findMissingRequiredFields}) are checked deterministically — date, time,
 * and party size must be present before the workflow can move to confirmation. Soft criteria
 * ({@link #suggestFollowUp}) use the model to suggest one useful follow-up question based on the
 * specific meal context.
 */
@Component
public class RequirementsAssessor {

  private final ChatClient chatClient;

  public RequirementsAssessor(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder == null ? null : chatClientBuilder.build();
  }

  /**
   * Identifies the required fields that must be present before the alignment phase can move to
   * confirmation. These are the hard gates: date, time, and party size. Returns an empty list when
   * all required fields are present.
   */
  public List<String> findMissingRequiredFields(Meal meal) {
    List<String> missing = new ArrayList<>();
    if (meal == null || meal.getDate() == null) {
      missing.add("Date");
    }
    if (meal == null || meal.getTime() == null) {
      missing.add("Time");
    }
    if (meal == null || meal.getPartySize() == null || meal.getPartySize() <= 0) {
      missing.add("Party Size");
    }
    return missing;
  }

  /**
   * Uses the model to suggest one optional follow-up question that a good executive assistant would
   * ask given the current requirements. The {@link ReplyComposer} handles the output naturally — if
   * the model has no suggestion it will say so and the composer will ignore it.
   */
  public String suggestFollowUp(UserRequirements requirements) {
    return chatClient
        .prompt()
        .system(
            """
            You are an executive assistant assessing business meal planning requirements.

            Given the current requirements, suggest ONE thing that would be most useful
            to ask about next. Think about what a thoughtful assistant would want to know
            given the specific context of this meal.

            Rules:
            - Return a single short suggestion (under 15 words).
            - If the requirements look complete enough, say so briefly.
            - Do not suggest things that are already captured in the requirements.
            - Prioritize what matters most for this specific meal context.
            - For example, a client dinner for 20 might need dress code or parking info,
              while a casual team lunch for 3 probably doesn't.
            """)
        .user(
            u ->
                u.text(
                        """
                    <requirements>
                    {requirements}
                    </requirements>

                    What is the single most useful thing to ask about next?
                    """)
                    .param("requirements", JsonUtils.toJson(requirements)))
        .call()
        .content();
  }
}
