package com.example.restaurant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MenuItem(
    String name,
    String description,
    BigDecimal price,
    String currency,
    List<String> dietaryTags,
    List<MenuVariant> variants) {}
