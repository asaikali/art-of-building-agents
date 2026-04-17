package com.example.restaurant;

public record PriceRange(String tier, Integer min, Integer max, String currency, String label) {}
