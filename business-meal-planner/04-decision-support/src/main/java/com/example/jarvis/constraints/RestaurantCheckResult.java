package com.example.jarvis.constraints;

import com.example.jarvis.constraints.deterministic.budget.BudgetPerPersonCheckResult;
import com.example.jarvis.constraints.deterministic.noise.NoiseLevelCheckResult;
import com.example.jarvis.constraints.deterministic.travel.TravelTimeCheckResult;
import com.example.jarvis.constraints.hybrid.dietary.DietarySuitabilityResult;
import com.example.jarvis.constraints.llmjudge.suitability.MealSuitabilityResult;

public record RestaurantCheckResult(
    RestaurantCandidate candidate,
    NoiseLevelCheckResult noiseLevel,
    BudgetPerPersonCheckResult budgetPerPerson,
    TravelTimeCheckResult travelTime,
    DietarySuitabilityResult dietarySuitability,
    MealSuitabilityResult mealSuitability) {}
