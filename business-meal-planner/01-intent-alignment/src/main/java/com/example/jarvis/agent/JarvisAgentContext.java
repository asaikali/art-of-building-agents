package com.example.jarvis.agent;

import com.example.agent.core.json.JsonUtils;
import com.example.agent.core.session.AgentContext;
import com.example.jarvis.requirements.UserRequirements;
import com.example.jarvis.requirements.alignment.AlignmentStatus;
import java.util.Map;

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

  public String toMarkdown() {
    return """
        ```json
        %s
        ```
        """
        .formatted(
            JsonUtils.toJson(Map.of("requirements", userRequirements, "status", status.label())));
  }
}
