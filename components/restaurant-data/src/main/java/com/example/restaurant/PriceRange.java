package com.example.restaurant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PriceRange(String tier, Integer min, Integer max, String currency, String label) {}
