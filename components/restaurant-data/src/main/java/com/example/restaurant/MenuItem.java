package com.example.restaurant;

import java.math.BigDecimal;
import java.util.List;

public record MenuItem(
    String name,
    String description,
    BigDecimal price,
    String currency,
    List<String> dietaryTags,
    List<MenuVariant> variants) {}
