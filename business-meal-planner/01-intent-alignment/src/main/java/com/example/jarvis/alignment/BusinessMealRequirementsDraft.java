package com.example.jarvis.alignment;

import java.util.List;

record BusinessMealRequirementsDraft(
    String intent,
    List<String> explicitConstraints,
    List<String> inferredConstraints,
    List<String> missingInformation,
    List<String> assumptions) {}
