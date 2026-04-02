package com.example.agents.judge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.agent.core", "com.example.agents.judge"})
public class JudgeApplication {

  public static void main(String[] args) {
    SpringApplication.run(JudgeApplication.class, args);
  }
}
