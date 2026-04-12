package com.example.jarvis.requirements.alignment;

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
 * <p>The pipeline has three steps:
 *
 * <ol>
 *   <li><b>Extract</b> — {@link RequirementsExtractor} uses the model to merge the user message
 *       into the current {@link UserRequirements}
 *   <li><b>Determine status</b> — deterministic logic checks what required fields are missing and
 *       whether the user confirmed, then decides the {@link AlignmentStatus}
 *   <li><b>Compose reply</b> — {@link ReplyComposer} uses the model to write a natural reply based
 *       on the status. {@link RequirementsAssessor#suggestFollowUp} is called only when the status
 *       is {@link AlignmentStatus#CONFIRMING_REQUIREMENTS}
 * </ol>
 */
@Service
public class RequirementsAligner {

  private static final Logger log = LoggerFactory.getLogger(RequirementsAligner.class);

  private final RequirementsExtractor extractor;
  private final RequirementsAssessor assessor;
  private final ReplyComposer composer;

  public RequirementsAligner(
      RequirementsExtractor extractor, RequirementsAssessor assessor, ReplyComposer composer) {
    this.extractor = extractor;
    this.assessor = assessor;
    this.composer = composer;
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
   */
  public Result processMessage(
      UserRequirements currentRequirements, AlignmentStatus currentStatus, String userMessage) {

    // Step 1: Extract — model merges the user message into the current requirements
    UserRequirements updatedRequirements = extractor.extract(currentRequirements, userMessage);

    // Step 2: Determine status based on what's missing and whether the user confirmed
    List<String> missingFields = assessor.findMissingRequiredFields(updatedRequirements.getMeal());
    AlignmentStatus updatedStatus =
        determineStatus(missingFields, currentStatus, currentRequirements, updatedRequirements);
    log.info("status | {} → {}", currentStatus.label(), updatedStatus.label());

    // Step 3: Compose reply — model writes a response appropriate to the status
    String reply = composeReply(updatedStatus, missingFields, updatedRequirements);
    log.info("reply | \"{}\"", reply);

    return new Result(updatedRequirements, missingFields, updatedStatus, reply);
  }

  // If required fields are missing we stay in gathering. If the user confirmed (requirements
  // unchanged after a confirmation prompt) we're done. Otherwise we ask the user to confirm.
  private AlignmentStatus determineStatus(
      List<String> missingFields,
      AlignmentStatus currentStatus,
      UserRequirements before,
      UserRequirements after) {
    if (!missingFields.isEmpty()) {
      return AlignmentStatus.GATHERING_REQUIREMENTS;
    }
    if (isConfirmation(currentStatus, before, after)) {
      return AlignmentStatus.REQUIREMENTS_CONFIRMED;
    }
    return AlignmentStatus.CONFIRMING_REQUIREMENTS;
  }

  private boolean isConfirmation(
      AlignmentStatus currentStatus, UserRequirements before, UserRequirements after) {
    return currentStatus == AlignmentStatus.CONFIRMING_REQUIREMENTS && isUnchanged(before, after);
  }

  private boolean isUnchanged(UserRequirements before, UserRequirements after) {
    return before.equals(after);
  }

  private String composeReply(
      AlignmentStatus status, List<String> missingFields, UserRequirements updatedRequirements) {
    return switch (status) {
      case GATHERING_REQUIREMENTS ->
          composer.askForMissingField(missingFields.getFirst(), updatedRequirements);
      case CONFIRMING_REQUIREMENTS -> {
        String suggestion = assessor.suggestFollowUp(updatedRequirements);
        yield composer.askForConfirmation(suggestion, updatedRequirements);
      }
      case REQUIREMENTS_CONFIRMED -> composer.acknowledgeConfirmation(updatedRequirements);
    };
  }
}
