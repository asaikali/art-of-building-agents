package com.example.jarvis.requirements.alignment;

import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.agent.RequirementStatus;
import com.example.jarvis.requirements.EventRequirements;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RequirementsCompletionPolicy {

  public List<String> missingCriticalFields(EventRequirements eventRequirements) {
    List<String> missing = new ArrayList<>();
    if (eventRequirements == null || eventRequirements.getDate() == null) {
      missing.add("Date");
    }
    if (eventRequirements == null || eventRequirements.getTime() == null) {
      missing.add("Time");
    }
    if (eventRequirements == null
        || eventRequirements.getPartySize() == null
        || eventRequirements.getPartySize() <= 0) {
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
