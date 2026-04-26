package com.example.jarvis.constraints;

/** Minimal handle to a restaurant being checked: just an id and a display name. */
public record RestaurantCandidate(String restaurantId, String name) {}
