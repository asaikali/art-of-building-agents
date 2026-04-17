package com.example.jarvis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example")
public class RestaurantPlanningApplication {

  public static void main(String[] args) {
    SpringApplication.run(RestaurantPlanningApplication.class, args);
  }
}
