package com.example.agents.turnlimits;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {"com.example.agent.core", "com.example.agents.turnlimits"})
public class TurnLimitsApplication {

  public static void main(String[] args) {
    SpringApplication.run(TurnLimitsApplication.class, args);
  }
}
