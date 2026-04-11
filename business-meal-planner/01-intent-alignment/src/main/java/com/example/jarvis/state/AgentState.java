package com.example.jarvis.state;

import com.example.jarvis.alignment.RequirementStatus;
import java.util.List;
import java.util.Optional;

public class AgentState {

  private UserGoals userGoals;
  private List<String> missingInformation = List.of();
  private List<String> assumptions = List.of();
  private RequirementStatus status = RequirementStatus.WAITING_FOR_CLARIFICATION;

  public Optional<UserGoals> userGoals() {
    return Optional.ofNullable(userGoals);
  }

  public void setUserGoals(UserGoals userGoals) {
    this.userGoals = userGoals;
  }

  public List<String> missingInformation() {
    return missingInformation;
  }

  public void setMissingInformation(List<String> missingInformation) {
    this.missingInformation =
        missingInformation == null ? List.of() : List.copyOf(missingInformation);
  }

  public List<String> assumptions() {
    return assumptions;
  }

  public void setAssumptions(List<String> assumptions) {
    this.assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
  }

  public RequirementStatus status() {
    return status;
  }

  public void setStatus(RequirementStatus status) {
    this.status = status;
  }
}
