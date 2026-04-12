package com.example.jarvis.agent;

import com.example.agent.core.json.JsonUtils;
import com.example.agent.core.session.AgentContext;
import com.example.jarvis.requirements.UserRequirements;
import com.example.jarvis.requirements.alignment.AlignmentStatus;
import java.util.List;

public class JarvisAgentContext implements AgentContext {

  private UserRequirements userRequirements = new UserRequirements();
  private AlignmentStatus status = AlignmentStatus.WAITING_FOR_CLARIFICATION;

  public UserRequirements getUserRequirements() {
    return userRequirements;
  }

  public void setUserRequirements(UserRequirements userRequirements) {
    this.userRequirements = userRequirements == null ? new UserRequirements() : userRequirements;
  }

  public AlignmentStatus getStatus() {
    return status;
  }

  public void setStatus(AlignmentStatus status) {
    this.status = status;
  }

  public String toMarkdown(List<String> missingRequiredFields) {
    return """
        ## Requirements
        ```json
        %s
        ```

        ## Missing Information
        %s

        ## Status
        %s
        """
        .formatted(
            JsonUtils.toJson(userRequirements), renderList(missingRequiredFields), status.label());
  }

  private static String renderList(List<String> items) {
    if (items == null || items.isEmpty()) {
      return "- None";
    }
    return items.stream()
        .map(item -> "- " + item)
        .collect(java.util.stream.Collectors.joining("\n"));
  }
}
