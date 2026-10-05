package com.example.foundations.tools;

import java.time.LocalDateTime;

public record WeatherResponse(
    String city,
    double temperature,
    String weatherCondition,
    int humidity,
    double windSpeed,
    String windDirection,
    LocalDateTime timeStamp) {}
