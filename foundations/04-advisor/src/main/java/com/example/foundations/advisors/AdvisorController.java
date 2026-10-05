package com.example.foundations.advisors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/advisors", produces = MediaType.TEXT_PLAIN_VALUE)
public class AdvisorController {

  private final ChatClient chatClient;
  private final ChatClient toolChatClient;
  private final WeatherService weatherService;
  private final ActivityService activityService;

  public AdvisorController(
      ChatClient.Builder builder,
      ToolCallingManager toolCallingManager,
      WeatherService weatherService,
      ActivityService activityService) {
    this.chatClient = builder.build();
    this.toolChatClient =
        builder
            .clone()
            .defaultAdvisors(
                new TraceAdvisor("Turn", ToolCallingAdvisor.DEFAULT_ORDER - 1),
                ToolCallingAdvisor.builder().toolCallingManager(toolCallingManager).build(),
                new TraceAdvisor("Model", ToolCallingAdvisor.DEFAULT_ORDER + 1))
            .build();
    this.weatherService = weatherService;
    this.activityService = activityService;
  }

  // 1. Advisors run by order, then unwind as the response returns.
  @GetMapping("/order")
  public String order() {
    return chatClient
        .prompt()
        .advisors(new TraceAdvisor("Second", 1), new TraceAdvisor("First", 0))
        .user("Why does it rain? Answer in one sentence.")
        .call()
        .content();
  }

  // 2. Turn runs once; Model runs for each request in the tool loop.
  @GetMapping("/weather")
  public String weather(@RequestParam(defaultValue = "Toronto") String city) {
    return toolChatClient
        .prompt()
        .tools(weatherService)
        .user(u -> u.text("What is the current weather in {city}?").param("city", city))
        .call()
        .content();
  }

  // 3. The same advisor chain observes two dependent tools and three model calls.
  @GetMapping("/activities")
  public String activities(@RequestParam(defaultValue = "Toronto") String city) {
    return toolChatClient
        .prompt()
        .tools(weatherService, activityService)
        .user(
            u ->
                u.text(
                        """
                        What should I do in {city} today?
                        First check the current weather, then use the activity finder
                        with the returned weather condition and temperature.
                        Base your recommendations on the activities it returns.
                        """)
                    .param("city", city))
        .call()
        .content();
  }
}
