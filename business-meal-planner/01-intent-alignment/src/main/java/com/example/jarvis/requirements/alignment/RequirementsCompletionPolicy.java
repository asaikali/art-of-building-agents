package com.example.jarvis.requirements.alignment;

import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.requirements.Meal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RequirementsCompletionPolicy {

  public List<String> missingCriticalFields(Meal meal) {
    List<String> missing = new ArrayList<>();
    if (meal == null || meal.getDate() == null) {
      missing.add("Date");
    }
    if (meal == null || meal.getTime() == null) {
      missing.add("Time");
    }
    if (meal == null || meal.getPartySize() == null || meal.getPartySize() <= 0) {
      missing.add("Party Size");
    }
    return missing;
  }

  public RequirementStatus decideStatus(JarvisAgentContext context) {
    if (!context.getMissingInformation().isEmpty()) {
      return RequirementStatus.WAITING_FOR_CLARIFICATION;
    }
    return RequirementStatus.WAITING_FOR_CONFIRMATION;
  }
}
