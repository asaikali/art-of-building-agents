package com.example.agents.toolcalling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {"com.example.agent.core", "com.example.agents.toolcalling"})
public class ToolCallingApplication {

  public static void main(String[] args) {
    SpringApplication.run(ToolCallingApplication.class, args);
  }
}
