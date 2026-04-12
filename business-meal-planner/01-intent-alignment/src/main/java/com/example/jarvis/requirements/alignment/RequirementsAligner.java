package com.example.jarvis.requirements.alignment;

import com.example.agent.core.chat.AgentMessage;
import com.example.jarvis.agent.JarvisAgentContext;
import com.example.jarvis.requirements.UserRequirements;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RequirementsAligner {

  private final RequirementsExtractor requirementsExtractor;
  private final RequirementsCompletionPolicy requirementsCompletionPolicy;
  private final RequirementsReplyWriter requirementsReplyWriter;

  public RequirementsAligner(
      RequirementsExtractor requirementsExtractor,
      RequirementsCompletionPolicy requirementsCompletionPolicy,
      RequirementsReplyWriter requirementsReplyWriter) {
    this.requirementsExtractor = requirementsExtractor;
    this.requirementsCompletionPolicy = requirementsCompletionPolicy;
    this.requirementsReplyWriter = requirementsReplyWriter;
  }

  public Result processMessage(
      JarvisAgentContext context, String userMessage, List<AgentMessage> conversationHistory) {

    // Step 1: Extract — model maps user message into updated requirements
    UserRequirements updated =
        requirementsExtractor.extract(context.getUserRequirements(), userMessage);

    // Step 2: Check — deterministic policy evaluation
    boolean userConfirmed =
        context.getStatus() == RequirementStatus.WAITING_FOR_CONFIRMATION
            && updated.equals(context.getUserRequirements());

    RequirementsCompletionPolicy.CompletionResult check =
        requirementsCompletionPolicy.evaluate(updated, userConfirmed);

    // Step 3: Directive — code picks what the reply must accomplish
    ReplyDirective directive =
        new ReplyDirective(
            check.status(), check.missingCriticalFields(), check.suggestedFollowUps(), updated);

    // Step 4: Reply — model writes a natural response
    String reply = requirementsReplyWriter.writeReply(directive, conversationHistory);

    // Update context
    context.setUserRequirements(updated);
    context.setMissingInformation(check.missingCriticalFields());
    context.setStatus(check.status());

    return new Result(context, reply, chooseEventName(check.status()));
  }

  private String chooseEventName(RequirementStatus status) {
    return switch (status) {
      case WAITING_FOR_CLARIFICATION -> "clarification-requested";
      case WAITING_FOR_CONFIRMATION -> "plan-updated";
      case REQUIREMENTS_CONFIRMED -> "requirements-confirmed";
    };
  }

  public record Result(JarvisAgentContext state, String assistantReply, String eventName) {}
}
