package com.example.jarvis.alignment;

public interface IntentAlignmentModelClient {

  BusinessMealRequirements createInitialPlan(String userMessage, RequirementStatus status);

  BusinessMealRequirements revisePlan(
      BusinessMealRequirements existingRequirements, String userMessage, RequirementStatus status);

  String summarizePlan(BusinessMealRequirements requirements);
}
