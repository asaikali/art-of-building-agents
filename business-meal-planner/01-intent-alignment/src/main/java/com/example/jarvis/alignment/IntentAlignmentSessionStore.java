package com.example.jarvis.alignment;

import com.example.agent.core.session.SessionId;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class IntentAlignmentSessionStore {

  private final ConcurrentHashMap<SessionId, BusinessMealRequirements> requirementsBySession =
      new ConcurrentHashMap<>();

  public Optional<BusinessMealRequirements> findPlan(SessionId sessionId) {
    return Optional.ofNullable(requirementsBySession.get(sessionId));
  }

  public void savePlan(SessionId sessionId, BusinessMealRequirements requirements) {
    requirementsBySession.put(sessionId, requirements);
  }
}
