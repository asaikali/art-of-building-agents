package com.example.foundations.advisors;

import java.time.LocalDateTime;

public record WeatherResponse(
    String city,
    double temperature,
    String weatherCondition,
    int humidity,
    double windSpeed,
    String windDirection,
    LocalDateTime timeStamp) {}
