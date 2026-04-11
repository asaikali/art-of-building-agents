package com.example.jarvis.alignment;

public record IntentAlignmentTurnResult(
    BusinessMealRequirements requirements, String assistantReply, IntentAlignmentAction action) {}
