package com.example.agents.subagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.agent.core", "com.example.agents.subagent"})
public class SubagentApplication {

  public static void main(String[] args) {
    SpringApplication.run(SubagentApplication.class, args);
  }
}
