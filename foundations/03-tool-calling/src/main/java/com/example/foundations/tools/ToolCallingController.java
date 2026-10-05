package com.example.foundations.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/tools", produces = MediaType.TEXT_PLAIN_VALUE)
public class ToolCallingController {

  private final ChatClient chatClient;
  private final WeatherService weatherService;
  private final ActivityService activityService;

  public ToolCallingController(
      ChatClient.Builder builder, WeatherService weatherService, ActivityService activityService) {
    this.chatClient = builder.build();
    this.weatherService = weatherService;
    this.activityService = activityService;
  }

  // 1. One city needs one weather tool call.
  @GetMapping("/weather")
  public String weather(@RequestParam(defaultValue = "Toronto") String city) {
    return chatClient
        .prompt()
        .tools(weatherService)
        .user(u -> u.text("What is the current weather in {city}?").param("city", city))
        .call()
        .content();
  }

  // 2. The model looks up each city's weather before composing packing advice.
  @GetMapping("/pack")
  public String pack(@RequestParam(defaultValue = "Toronto and Paris") String cities) {
    return chatClient
        .prompt()
        .tools(weatherService)
        .user(
            u ->
                u.text(
                        """
                        I am traveling to {cities}. What clothes should I pack.
                        """)
                    .param("cities", cities))
        .call()
        .content();
  }

  // 3. The activity finder needs the result of the weather tool first.
  @GetMapping("/activities")
  public String activities(@RequestParam(defaultValue = "Toronto") String city) {
    return chatClient
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
