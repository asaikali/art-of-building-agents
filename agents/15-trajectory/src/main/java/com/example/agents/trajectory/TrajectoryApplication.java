package com.example.agents.trajectory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {"com.example.agent.core", "com.example.agents.trajectory"})
public class TrajectoryApplication {

  public static void main(String[] args) {
    SpringApplication.run(TrajectoryApplication.class, args);
  }
}
