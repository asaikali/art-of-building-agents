package com.example.agents.hitl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.agent.core", "com.example.agents.hitl"})
public class HumanInTheLoopApplication {

  public static void main(String[] args) {
    SpringApplication.run(HumanInTheLoopApplication.class, args);
  }
}
