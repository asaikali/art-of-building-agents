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

  public ToolCallingController(ChatClient.Builder builder, WeatherService weatherService) {
    this.chatClient = builder.build();
    this.weatherService = weatherService;
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
}
