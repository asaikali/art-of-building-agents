package com.example.restaurant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantMenu(String restaurantId, List<MenuSection> menuSections) {}
