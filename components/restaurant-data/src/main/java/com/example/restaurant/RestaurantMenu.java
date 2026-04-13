package com.example.restaurant;

import java.util.List;

public record RestaurantMenu(String restaurantId, List<MenuSection> menuSections) {}
