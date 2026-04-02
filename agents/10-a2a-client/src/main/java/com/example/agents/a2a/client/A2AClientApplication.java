package com.example.agents.a2a.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {"com.example.agent.core", "com.example.agents.a2a.client"})
public class A2AClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(A2AClientApplication.class, args);
  }
}
