package com.example.jarvis.agent;

import com.example.agent.core.session.AgentContext;
import com.example.jarvis.requirements.UserRequirements;
import com.example.jarvis.requirements.alignment.AlignmentStatus;

public class JarvisAgentContext implements AgentContext {

  private UserRequirements userRequirements = new UserRequirements();
  private AlignmentStatus alignmentStatus = AlignmentStatus.GATHERING_REQUIREMENTS;
  private WorkflowPhase phase = WorkflowPhase.ALIGNMENT;
  private String shortlist;

  public UserRequirements getUserRequirements() {
    return userRequirements;
  }

  public void setUserRequirements(UserRequirements userRequirements) {
    this.userRequirements = userRequirements == null ? new UserRequirements() : userRequirements;
  }

  public AlignmentStatus getAlignmentStatus() {
    return alignmentStatus;
  }

  public void setAlignmentStatus(AlignmentStatus alignmentStatus) {
    this.alignmentStatus = alignmentStatus;
  }

  public WorkflowPhase getPhase() {
    return phase;
  }

  public void setPhase(WorkflowPhase phase) {
    this.phase = phase;
  }

  public String getShortlist() {
    return shortlist;
  }

  public void setShortlist(String shortlist) {
    this.shortlist = shortlist;
  }
}
