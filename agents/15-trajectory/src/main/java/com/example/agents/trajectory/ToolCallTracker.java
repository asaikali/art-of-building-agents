package com.example.agents.trajectory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-local tracker for tool call names. Tools call {@link #record(String)} when invoked. The
 * handler reads and clears the list after each conversation.
 */
public final class ToolCallTracker {

  private static final ThreadLocal<List<String>> CALLS = ThreadLocal.withInitial(ArrayList::new);

  private ToolCallTracker() {}

  public static void record(String toolName) {
    CALLS.get().add(toolName);
  }

  public static List<String> getAndClear() {
    List<String> result = Collections.unmodifiableList(new ArrayList<>(CALLS.get()));
    CALLS.get().clear();
    return result;
  }
}
