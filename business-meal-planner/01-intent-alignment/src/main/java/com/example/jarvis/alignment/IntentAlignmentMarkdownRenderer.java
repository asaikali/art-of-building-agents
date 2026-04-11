package com.example.jarvis.alignment;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IntentAlignmentMarkdownRenderer {

  public String render(BusinessMealRequirements requirements) {
    return """
        ## Intent
        %s

        ## Explicit Constraints
        %s

        ## Inferred Constraints
        %s

        ## Missing Information
        %s

        ## Assumptions
        %s

        ## Status
        %s
        """
        .formatted(
            requirements.intent(),
            renderList(requirements.explicitConstraints()),
            renderList(requirements.inferredConstraints()),
            renderList(requirements.missingInformation()),
            renderList(requirements.assumptions()),
            requirements.status().label());
  }

  private String renderList(List<String> items) {
    if (items.isEmpty()) {
      return "- None";
    }
    return items.stream()
        .map(item -> "- " + item)
        .collect(java.util.stream.Collectors.joining("\n"));
  }
}
