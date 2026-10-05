package com.example.jarvis.search;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.UserRequirements;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Uses the tool calling introduced in foundations/03-tool-calling to discover candidates. */
@Service
public class RestaurantSearcher {

  private final ChatClient chatClient;
  private final RestaurantSearchTools searchTools;

  public RestaurantSearcher(ChatClient.Builder builder, RestaurantSearchTools searchTools) {
    this.chatClient = builder.build();
    this.searchTools = searchTools;
  }

  public String search(UserRequirements confirmedRequirements) {
    return chatClient
        .prompt()
        .system(
            """
            You are Jarvis, a warm business meal planning assistant.
            The user has confirmed their requirements. Search for available restaurants
            using the date, time, and party size. Use a neighborhood preference if given.
            Always use the search tool before presenting restaurants.

            Present up to five returned restaurants as available candidates, with their
            names and neighborhoods. Only present restaurants returned by the tool.
            The tool establishes availability only. Do not infer suitability from a name
            or use general knowledge to claim a restaurant meets the other requirements.
            Explain that budget, noise, dietary needs, travel time, and occasion suitability
            still need to be checked. Do not describe these candidates as verified matches.
            If no restaurants are available, explain that and ask whether the user wants
            to change the date, time, party size, or neighborhood.
            Write in natural language, without internal tool names or implementation details.
            """)
        .tools(searchTools)
        .user(
            u ->
                u.text(
                        "Find available restaurants for these confirmed requirements:\n{requirements}")
                    .param("requirements", JsonUtils.toJson(confirmedRequirements)))
        .call()
        .content();
  }
}
