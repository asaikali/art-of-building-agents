package com.example.jarvis.requirements.alignment;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.requirements.UserRequirements;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the alignment pipeline that turns a user message into an updated set of requirements
 * and a natural reply. Called by {@link com.example.jarvis.agent.JarvisAgentHandler} on each
 * message.
 *
 * <p>The pipeline coordinates three collaborators:
 *
 * <ol>
 *   <li>{@link RequirementsExtractor} — uses the model to merge the user message into the current
 *       {@link UserRequirements}
 *   <li>{@link RequirementsAssessor} — checks what required fields are missing and suggests a
 *       follow-up
 *   <li>{@link ReplyComposer} — uses the model to write a natural reply based on the current {@link
 *       AlignmentStatus}
 * </ol>
 *
 * <p>The aligner itself decides the workflow status (step 3) and picks which composer method to
 * call. It returns a {@link Result} with the updated requirements, status, and reply — the handler
 * applies these to the session context.
 */
@Service
public class RequirementsAligner {

  private static final Logger log = LoggerFactory.getLogger(RequirementsAligner.class);

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
      AlignmentStatus status,
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
      UserRequirements currentRequirements, AlignmentStatus currentStatus, String userMessage) {

    // Step 1: Extract — model maps user message into updated requirements
    UserRequirements updated = requirementsExtractor.extract(currentRequirements, userMessage);
    log.info("[Jarvis:Aligner] step1-extract | updatedRequirements={}", JsonUtils.toJson(updated));

    // Step 2: Assess — deterministic hard gates + model-based follow-up suggestion
    List<String> missing = requirementsAssessor.findMissingRequiredFields(updated.getMeal());
    String suggestion = requirementsAssessor.suggestFollowUp(updated);
    boolean userConfirmed =
        currentStatus == AlignmentStatus.WAITING_FOR_CONFIRMATION
            && updated.equals(currentRequirements);
    log.info(
        "[Jarvis:Aligner] step2-assess | missingFields={} | userConfirmed={} | suggestion=\"{}\"",
        missing,
        userConfirmed,
        suggestion);

    // Step 3: Status — decide the workflow status
    AlignmentStatus status = assessStatus(missing, userConfirmed);
    log.info("[Jarvis:Aligner] step3-status | {} → {}", currentStatus.label(), status.label());

    // Step 4: Reply — compose a natural response based on the status
    String reply =
        switch (status) {
          case WAITING_FOR_CLARIFICATION ->
              replyComposer.askForMissingField(missing.getFirst(), updated);
          case WAITING_FOR_CONFIRMATION -> replyComposer.askForConfirmation(suggestion, updated);
          case REQUIREMENTS_CONFIRMED -> replyComposer.acknowledgeConfirmation(updated);
        };
    log.info("[Jarvis:Aligner] step4-reply | reply=\"{}\"", reply);

    return new Result(updated, missing, status, reply);
  }

  private AlignmentStatus assessStatus(List<String> missingRequiredFields, boolean userConfirmed) {
    if (!missingRequiredFields.isEmpty()) {
      return AlignmentStatus.WAITING_FOR_CLARIFICATION;
    }
    if (userConfirmed) {
      return AlignmentStatus.REQUIREMENTS_CONFIRMED;
    }
    return AlignmentStatus.WAITING_FOR_CONFIRMATION;
  }
}
