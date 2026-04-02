package com.example.agents.mcpclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {"com.example.agent.core", "com.example.agents.mcpclient"})
public class McpClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(McpClientApplication.class, args);
  }
}
