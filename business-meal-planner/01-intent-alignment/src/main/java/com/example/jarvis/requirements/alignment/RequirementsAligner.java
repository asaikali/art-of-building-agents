package com.example.jarvis.requirements.alignment;

import com.example.agent.core.chat.AgentMessage;
import com.example.jarvis.requirements.UserRequirements;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RequirementsAligner {

  private final RequirementsExtractor requirementsExtractor;
  private final RequirementsCompletenessChecker requirementsCompletenessChecker;
  private final RequirementsReplyWriter requirementsReplyWriter;

  public RequirementsAligner(
      RequirementsExtractor requirementsExtractor,
      RequirementsCompletenessChecker requirementsCompletenessChecker,
      RequirementsReplyWriter requirementsReplyWriter) {
    this.requirementsExtractor = requirementsExtractor;
    this.requirementsCompletenessChecker = requirementsCompletenessChecker;
    this.requirementsReplyWriter = requirementsReplyWriter;
  }

  /** The computed outputs of a single alignment turn. */
  public record Result(
      UserRequirements updatedRequirements,
      RequirementsCompletenessChecker.CompletionResult check,
      String reply) {}

  /**
   * Processes one user message through the alignment pipeline. Called by {@link
   * com.example.jarvis.agent.JarvisAgentHandler} each time the user sends a message. The handler
   * passes in the current requirements and status, and applies the returned {@link Result} to the
   * session context.
   *
   * <p>The pipeline has four steps:
   *
   * <ol>
   *   <li><b>Extract</b> — the model merges the user message into the current requirements
   *   <li><b>Check</b> — deterministic checker evaluates completeness and suggests follow-ups
   *   <li><b>Directive</b> — code picks what the reply must accomplish based on the check result
   *   <li><b>Reply</b> — the model writes a natural response following the directive
   * </ol>
   */
  public Result processMessage(
      UserRequirements currentRequirements,
      RequirementStatus currentStatus,
      String userMessage,
      List<AgentMessage> conversationHistory) {

    // Step 1: Extract — model maps user message into updated requirements
    UserRequirements updated = requirementsExtractor.extract(currentRequirements, userMessage);

    // Step 2: Check — deterministic policy evaluation
    boolean userConfirmed =
        currentStatus == RequirementStatus.WAITING_FOR_CONFIRMATION
            && updated.equals(currentRequirements);

    RequirementsCompletenessChecker.CompletionResult check =
        requirementsCompletenessChecker.evaluate(updated, userConfirmed);

    // Step 3: Directive — code picks what the reply must accomplish
    ReplyDirective directive =
        new ReplyDirective(
            check.status(), check.missingCriticalFields(), check.suggestedFollowUps(), updated);

    // Step 4: Reply — model writes a natural response
    String reply = requirementsReplyWriter.writeReply(directive, conversationHistory);

    return new Result(updated, check, reply);
  }
}
