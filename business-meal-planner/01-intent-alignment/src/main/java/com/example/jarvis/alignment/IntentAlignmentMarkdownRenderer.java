package com.example.jarvis.alignment;

import com.example.jarvis.state.AgentState;
import com.example.jarvis.state.UserGoals;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IntentAlignmentMarkdownRenderer {

  public String render(AgentState state) {
    UserGoals userGoals =
        state
            .userGoals()
            .orElse(
                new UserGoals("Clarify the business meal request.", null, null, null, List.of()));
    return """
        ## Intent
        %s

        ## Minimum Requirements
        - Date: %s
        - Time: %s
        - Party Size: %s

        ## Constraints
        %s

        ## Assumptions
        %s

        ## Missing Information
        %s

        ## Status
        %s
        """
        .formatted(
            userGoals.getIntent(),
            renderValue(userGoals.getDate()),
            renderValue(userGoals.getTime()),
            renderValue(userGoals.getPartySize()),
            renderList(userGoals.getConstraints()),
            renderList(state.assumptions()),
            renderList(state.missingInformation()),
            state.status().label());
  }

  private String renderValue(Object value) {
    return value == null ? "Missing" : value.toString();
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
