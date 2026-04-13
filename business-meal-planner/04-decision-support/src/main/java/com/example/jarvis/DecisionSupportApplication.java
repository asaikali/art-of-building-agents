package com.example.jarvis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example")
public class DecisionSupportApplication {

  public static void main(String[] args) {
    SpringApplication.run(DecisionSupportApplication.class, args);
  }
}
