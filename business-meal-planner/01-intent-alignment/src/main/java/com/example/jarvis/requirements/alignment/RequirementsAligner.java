package com.example.jarvis.requirements.alignment;

import com.example.agent.core.chat.AgentMessage;
import com.example.jarvis.requirements.UserRequirements;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RequirementsAligner {

  private final RequirementsExtractor requirementsExtractor;
  private final RequirementsAssessor requirementsAssessor;
  private final ReplyComposer replyComposer;

  public RequirementsAligner(
      RequirementsExtractor requirementsExtractor,
      RequirementsAssessor requirementsAssessor,
      ReplyComposer replyComposer) {
    this.requirementsExtractor = requirementsExtractor;
    this.requirementsAssessor = requirementsAssessor;
    this.replyComposer = replyComposer;
  }

  /** The computed outputs of a single alignment turn. */
  public record Result(
      UserRequirements updatedRequirements,
      List<String> missingRequiredFields,
      RequirementStatus status,
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
   *   <li><b>Assess</b> — deterministic check for required fields, model suggests a follow-up
   *   <li><b>Status</b> — code decides the workflow status based on the assessment
   *   <li><b>Reply</b> — the model composes a natural response based on the status
   * </ol>
   */
  public Result processMessage(
      UserRequirements currentRequirements,
      RequirementStatus currentStatus,
      String userMessage,
      List<AgentMessage> conversationHistory) {

    // Step 1: Extract — model maps user message into updated requirements
    UserRequirements updated = requirementsExtractor.extract(currentRequirements, userMessage);

    // Step 2: Assess — deterministic hard gates + model-based follow-up suggestion
    List<String> missing = requirementsAssessor.findMissingRequiredFields(updated.getMeal());
    String suggestion = requirementsAssessor.suggestFollowUp(updated);
    boolean userConfirmed =
        currentStatus == RequirementStatus.WAITING_FOR_CONFIRMATION
            && updated.equals(currentRequirements);

    // Step 3: Status — decide the workflow status
    RequirementStatus status = assessStatus(missing, userConfirmed);

    // Step 4: Reply — compose a natural response based on the status
    String reply =
        switch (status) {
          case WAITING_FOR_CLARIFICATION ->
              replyComposer.composeClarificationReply(
                  missing.getFirst(), updated, conversationHistory);
          case WAITING_FOR_CONFIRMATION ->
              replyComposer.composeConfirmationReply(suggestion, updated, conversationHistory);
          case REQUIREMENTS_CONFIRMED ->
              replyComposer.composeConfirmedReply(updated, conversationHistory);
        };

    return new Result(updated, missing, status, reply);
  }

  private RequirementStatus assessStatus(
      List<String> missingRequiredFields, boolean userConfirmed) {
    if (!missingRequiredFields.isEmpty()) {
      return RequirementStatus.WAITING_FOR_CLARIFICATION;
    }
    if (userConfirmed) {
      return RequirementStatus.REQUIREMENTS_CONFIRMED;
    }
    return RequirementStatus.WAITING_FOR_CONFIRMATION;
  }
}
