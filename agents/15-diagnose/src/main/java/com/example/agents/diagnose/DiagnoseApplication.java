package com.example.agents.diagnose;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.agent.core", "com.example.agents.diagnose"})
public class DiagnoseApplication {

  public static void main(String[] args) {
    SpringApplication.run(DiagnoseApplication.class, args);
  }
}
