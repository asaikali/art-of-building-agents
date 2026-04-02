package com.example.agents.subagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Sub-agent dispatcher — launches a specialized restaurant researcher.
 *
 * <p>When Jarvis calls {@code delegateToResearcher}, this creates a fresh {@link ChatClient} with
 * its own system prompt, tools, and context window. The sub-agent runs an independent agent loop
 * (via {@link ToolCallAdvisor}) and returns the result.
 *
 * <p><b>Key concept:</b> The sub-agent is just another ChatClient call. It has its own system
 * prompt optimized for research, its own tools, and its own conversation context — completely
 * independent from Jarvis's context.
 */
public class ResearcherTool {

  private static final Logger log = LoggerFactory.getLogger(ResearcherTool.class);

  private static final String RESEARCHER_PROMPT =
      """
      You are a restaurant researcher specializing in Barcelona business dinners.

      Your job: search for restaurants, verify they meet ALL constraints, and return
      a concise summary of qualifying options.

      ## Process
      1. Search for restaurants matching the criteria
      2. For EACH candidate, check expense policy AND dietary options
      3. Filter out any restaurant that fails a check
      4. Return a structured summary of qualifying restaurants

      ## Output format
      For each qualifying restaurant, include:
      - Name, neighborhood, cuisine
      - Price per person
      - Whether it's within expense policy
      - Dietary options available
      - Noise level

      Be thorough but concise. Only include restaurants that pass ALL checks.
      """;

  private final ChatModel chatModel;
  private final RestaurantTools restaurantTools;

  public ResearcherTool(ChatModel chatModel, RestaurantTools restaurantTools) {
    this.chatModel = chatModel;
    this.restaurantTools = restaurantTools;
  }

  @Tool(
      description =
          "Delegate restaurant research to a specialized sub-agent. "
              + "The researcher will search restaurants, verify expense policy, and check dietary "
              + "options. Provide clear requirements: neighborhood, budget per person, party size, "
              + "and any dietary needs.")
  public String delegateToResearcher(
      @ToolParam(
              description =
                  "Research requirements, e.g. 'Find restaurants in Eixample, "
                      + "budget 30 EUR/person, 4 guests, one vegetarian'")
          String requirements) {
    log.info("Launching researcher sub-agent for: {}", requirements);

    ChatClient researcher =
        ChatClient.builder(chatModel)
            .defaultSystem(RESEARCHER_PROMPT)
            .defaultTools(restaurantTools)
            .defaultAdvisors(ToolCallAdvisor.builder().build())
            .build();

    String result = researcher.prompt().user(requirements).call().content();

    log.info("Researcher sub-agent completed");
    return result;
  }
}
