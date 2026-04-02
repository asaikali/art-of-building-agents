package com.example.agents.memory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.agent.core", "com.example.agents.memory"})
public class MemoryApplication {

  public static void main(String[] args) {
    SpringApplication.run(MemoryApplication.class, args);
  }
}
