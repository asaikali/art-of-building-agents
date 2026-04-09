package com.example.agents.hooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.agent.core", "com.example.agents.hooks"})
public class HooksApplication {

  public static void main(String[] args) {
    SpringApplication.run(HooksApplication.class, args);
  }
}
