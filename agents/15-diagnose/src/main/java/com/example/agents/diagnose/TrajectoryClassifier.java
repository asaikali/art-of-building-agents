package com.example.agents.diagnose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Classifies tool calls into semantic states and computes trajectory metrics. */
public class TrajectoryClassifier {

  /** Semantic states for the Jarvis restaurant domain. */
  public enum SemanticState {
    SEARCH,
    CHECK_BUDGET,
    CHECK_DIETARY,
    BOOK,
    CLARIFY
  }

  /** Result of trajectory analysis. */
  public record TrajectoryAnalysis(
      List<SemanticState> sequence,
      Map<SemanticState, Integer> stateCounts,
      Map<String, Integer> transitionCounts,
      List<String> loops,
      String hotspot,
      double efficiency) {}

  private static final Map<String, SemanticState> TOOL_TO_STATE =
      Map.of(
          "searchRestaurants", SemanticState.SEARCH,
          "checkExpensePolicy", SemanticState.CHECK_BUDGET,
          "checkDietaryOptions", SemanticState.CHECK_DIETARY,
          "bookTable", SemanticState.BOOK);

  /**
   * Classify a list of tool call names into a trajectory analysis.
   *
   * @param toolCalls ordered list of tool function names invoked by the agent
   * @return trajectory analysis with sequence, counts, loops, hotspot, efficiency
   */
  public TrajectoryAnalysis classify(List<String> toolCalls) {
    // Map tool calls to semantic states
    List<SemanticState> sequence = new ArrayList<>();
    for (String tool : toolCalls) {
      SemanticState state = TOOL_TO_STATE.getOrDefault(tool, SemanticState.CLARIFY);
      sequence.add(state);
    }

    if (sequence.isEmpty()) {
      sequence.add(SemanticState.CLARIFY);
    }

    // Count states
    Map<SemanticState, Integer> stateCounts = new LinkedHashMap<>();
    for (SemanticState s : sequence) {
      stateCounts.merge(s, 1, Integer::sum);
    }

    // Count transitions
    Map<String, Integer> transitionCounts = new LinkedHashMap<>();
    for (int i = 0; i < sequence.size() - 1; i++) {
      String key = sequence.get(i) + "\u2192" + sequence.get(i + 1);
      transitionCounts.merge(key, 1, Integer::sum);
    }

    // Detect loops (same state appearing consecutively)
    List<String> loops = new ArrayList<>();
    int selfLoopCount = 0;
    for (int i = 0; i < sequence.size() - 1; i++) {
      if (sequence.get(i) == sequence.get(i + 1)) {
        selfLoopCount++;
        loops.add(
            String.format("%s called consecutively at positions %d-%d", sequence.get(i), i, i + 1));
      }
    }

    // Find hotspot: most-visited state
    String hotspot = null;
    int maxCount = 0;
    int hotspotSelfLoops = 0;
    for (Map.Entry<SemanticState, Integer> entry : stateCounts.entrySet()) {
      if (entry.getValue() > maxCount) {
        maxCount = entry.getValue();
        hotspot = entry.getKey().name();
        // Count self-loops for this state
        String selfKey = entry.getKey() + "\u2192" + entry.getKey();
        hotspotSelfLoops = transitionCounts.getOrDefault(selfKey, 0);
      }
    }

    if (hotspot != null && maxCount > 1) {
      hotspot = String.format("%s (%d visits, %d self-loops)", hotspot, maxCount, hotspotSelfLoops);
    }

    // Efficiency: unique productive states / total states
    // "Productive" = states that aren't self-loop repetitions
    long uniqueStates = stateCounts.size();
    double efficiency =
        sequence.size() > 0 ? (double) (sequence.size() - selfLoopCount) / sequence.size() : 1.0;

    return new TrajectoryAnalysis(
        sequence, stateCounts, transitionCounts, loops, hotspot, efficiency);
  }

  /** Format the analysis as a Markdown string for the Inspector state panel. */
  public String formatMarkdown(TrajectoryAnalysis analysis) {
    StringBuilder sb = new StringBuilder();
    sb.append("## Trajectory Analysis\n\n");

    // Sequence
    String seq =
        analysis.sequence().stream()
            .map(SemanticState::name)
            .collect(Collectors.joining(" \u2192 "));
    sb.append("**Sequence**: ").append(seq).append("\n\n");
    sb.append(
        String.format(
            "**States**: %d | **Loops**: %d | **Efficiency**: %.0f%%\n\n",
            analysis.sequence().size(), analysis.loops().size(), analysis.efficiency() * 100));

    // State table
    sb.append("| State | Count | Self-Loops |\n");
    sb.append("|-------|-------|------------|\n");
    for (Map.Entry<SemanticState, Integer> entry : analysis.stateCounts().entrySet()) {
      String selfKey = entry.getKey() + "\u2192" + entry.getKey();
      int selfLoops = analysis.transitionCounts().getOrDefault(selfKey, 0);
      sb.append("| ")
          .append(entry.getKey())
          .append(" | ")
          .append(entry.getValue())
          .append(" | ")
          .append(selfLoops)
          .append(" |\n");
    }

    // Hotspot warning
    if (analysis.hotspot() != null && analysis.loops().size() > 0) {
      sb.append("\n\u26a0 **Hotspot**: ").append(analysis.hotspot()).append("\n");
    }

    return sb.toString();
  }
}
