package com.example.agents.journal;

import io.github.markpollack.journal.Run;
import io.github.markpollack.journal.event.CustomEvent;
import io.github.markpollack.workflow.core.LoopState;
import io.github.markpollack.workflow.core.TerminationReason;
import io.github.markpollack.workflow.patterns.advisor.AgentLoopListener;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges AgentLoopAdvisor events into agent-journal.
 *
 * <p>This is the key new concept in Step 05: every turn of the agent loop gets recorded as a
 * journal event. The same JSONL data later feeds trajectory analysis, heatmaps, and judge scoring.
 */
public class JournalLoopListener implements AgentLoopListener {

  private static final Logger log = LoggerFactory.getLogger(JournalLoopListener.class);

  private final Run run;

  public JournalLoopListener(Run run) {
    this.run = run;
  }

  @Override
  public void onLoopStarted(String runId, String userMessage) {
    log.info("Journal: loop started, runId={}", runId);
    run.logEvent(
        CustomEvent.of("loop_started", Map.of("runId", runId, "userMessage", userMessage)));
  }

  @Override
  public void onTurnStarted(String runId, int turn) {
    log.info("Journal: turn {} started", turn);
    run.logEvent(CustomEvent.of("turn_started", Map.of("runId", runId, "turn", turn)));
  }

  @Override
  public void onTurnCompleted(String runId, int turn, TerminationReason reason) {
    log.info("Journal: turn {} completed, reason={}", turn, reason);
    var data = new java.util.HashMap<String, Object>();
    data.put("runId", runId);
    data.put("turn", turn);
    if (reason != null) {
      data.put("terminationReason", reason.name());
    }
    run.logEvent(CustomEvent.of("turn_completed", data));
  }

  @Override
  public void onLoopCompleted(String runId, LoopState finalState, TerminationReason reason) {
    log.info(
        "Journal: loop completed — turns={}, tokens={}, cost=${}, reason={}",
        finalState.currentTurn(),
        finalState.totalTokensUsed(),
        String.format("%.4f", finalState.estimatedCost()),
        reason);
    run.logEvent(
        CustomEvent.of(
            "loop_completed",
            Map.of(
                "runId", runId,
                "turnsCompleted", finalState.currentTurn(),
                "totalTokens", finalState.totalTokensUsed(),
                "estimatedCost", finalState.estimatedCost(),
                "reason", reason.name())));
    run.setSummary("turnsCompleted", finalState.currentTurn());
    run.setSummary("totalTokens", finalState.totalTokensUsed());
    run.setSummary("estimatedCost", finalState.estimatedCost());
    run.setSummary("terminationReason", reason.name());
  }

  @Override
  public void onLoopFailed(String runId, LoopState state, Throwable error) {
    log.error("Journal: loop failed at turn {}", state.currentTurn(), error);
    run.fail(error);
  }
}
