package com.example.restaurant;

public record Restaurant(
    String id,
    String name,
    String address,
    String neighborhood,
    String url,
    String menuUrl,
    String menuPdfUrl,
    String noiseLevel,
    PriceRange priceRangePerPerson,
    String description) {}
