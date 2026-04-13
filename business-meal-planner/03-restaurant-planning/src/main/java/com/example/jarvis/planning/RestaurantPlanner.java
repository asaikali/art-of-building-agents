package com.example.jarvis.planning;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.UserRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the restaurant planning sequence. After Phase 1 confirms requirements, this service
 * uses Spring AI tool calling to discover and evaluate restaurant candidates.
 *
 * <p>The model drives the planning by calling tools: first finding available restaurants, then
 * evaluating them against the confirmed constraints. Spring AI's {@link ToolCallAdvisor} handles
 * tool execution automatically.
 */
@Service
public class RestaurantPlanner {

  private static final Logger log = LoggerFactory.getLogger(RestaurantPlanner.class);

  private final ChatClient chatClient;
  private final PlanningTools planningTools;

  public RestaurantPlanner(ChatClient.Builder chatClientBuilder, PlanningTools planningTools) {
    this.planningTools = planningTools;
    this.chatClient =
        chatClientBuilder
            .defaultSystem(
                """
                You are Jarvis, a warm and professional business meal planning assistant.
                The user's requirements have been confirmed and you now need to find
                suitable restaurants.

                Strategy:
                1. Search for available restaurants matching the date, time, and party size
                   from the confirmed requirements. If a neighborhood preference is provided,
                   use it to narrow the search.
                2. Pick the most promising candidates (up to 5) and evaluate each one against
                   all the confirmed constraints.
                3. Review the evaluation results:
                   - FAIL on noise, budget, or dietary = hard violation, do not recommend
                   - MAYBE or UNSURE = soft concern, mention it but don't disqualify
                   - PASS = constraint satisfied
                4. Only present restaurants that pass all hard constraints. Do not mention
                   restaurants that failed — the user doesn't need to know about them.
                   Note any soft concerns for the restaurants you do recommend.
                   If no restaurant passes all hard constraints, explain what made it
                   difficult to find a match and suggest which constraint the user might
                   relax.

                Tone:
                - Write like a helpful concierge, not a constraint checker.
                - Never mention PASS, FAIL, UNSURE, MAYBE, or internal check names.
                - Describe strengths and concerns in plain language.
                - For example say "great vegetarian options" not "dietary suitability: PASS".
                - If a restaurant doesn't fit, explain why naturally without jargon.
                """)
            .defaultTools(planningTools)
            .defaultAdvisors(ToolCallAdvisor.builder().build())
            .build();
  }

  public String plan(UserRequirements confirmedRequirements) {
    log.info("plan | starting with requirements={}", JsonUtils.toJson(confirmedRequirements));

    // Make requirements available to the checkRestaurantCandidate tool
    planningTools.setCurrentRequirements(confirmedRequirements);

    String reply =
        chatClient
            .prompt()
            .user(
                u ->
                    u.text(
                            """
                Find restaurants for the following confirmed requirements:

                {requirements}
                """)
                        .param("requirements", JsonUtils.toJson(confirmedRequirements)))
            .call()
            .content();

    log.info("plan | result={}", reply);
    return reply;
  }
}
