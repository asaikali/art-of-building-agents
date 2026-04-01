package com.example.agents.guardrails;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {"com.example.agent.core", "com.example.agents.guardrails"})
public class GuardrailsApplication {

  public static void main(String[] args) {
    SpringApplication.run(GuardrailsApplication.class, args);
  }
}
