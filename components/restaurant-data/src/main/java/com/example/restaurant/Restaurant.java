package com.example.restaurant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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
