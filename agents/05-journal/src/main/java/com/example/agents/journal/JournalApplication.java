package com.example.agents.journal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.agent.core", "com.example.agents.journal"})
public class JournalApplication {

  public static void main(String[] args) {
    SpringApplication.run(JournalApplication.class, args);
  }
}
