package com.example.jarvis.requirements.alignment;

import com.example.jarvis.requirements.UserRequirements;
import java.util.List;

/**
 * Data carrier that tells the reply writer what the assistant response must accomplish.
 *
 * <p>Constructed by the alignment loop after the completion policy runs. The code controls
 * <em>what</em> to say; the reply writer controls <em>how</em> to say it.
 */
public record ReplyDirective(
    RequirementStatus status,
    List<String> missingCriticalFields,
    List<String> suggestedFollowUps,
    UserRequirements currentRequirements) {}
