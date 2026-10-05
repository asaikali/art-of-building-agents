package com.example.spy;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

/**
 * Observes Spring AI's existing chain; never inserts advisors or modifies a request or response.
 */
public class SpyObservationHandler implements ObservationHandler<Observation.Context> {
  private static final String HANDLE = SpyObservationHandler.class.getName();
  private static final Logger log = LoggerFactory.getLogger(SpyObservationHandler.class);
  private final SpyTraceStore store;

  public SpyObservationHandler(SpyTraceStore store) {
    this.store = store;
  }

  @Override
  public boolean supportsContext(Observation.Context context) {
    return context instanceof ChatClientObservationContext
        || context instanceof AdvisorObservationContext
        || context instanceof ChatModelObservationContext
        || context instanceof ToolCallingObservationContext;
  }

  @Override
  public void onStart(Observation.Context context) {
    try {
      SpyTraceStore.Handle parent = null;
      var ancestor = context.getParentObservation();
      while (ancestor != null && parent == null) {
        parent = ancestor.getContextView().get(HANDLE);
        ancestor = ancestor.getContextView().getParentObservation();
      }
      var handle = store.begin(parent, describe(context));
      if (handle != null) context.put(HANDLE, handle);
    } catch (RuntimeException exception) {
      // Recording must never break the invocation being taught.
      log.debug(
          "Spy could not record an observation start ({})", exception.getClass().getSimpleName());
    }
  }

  @Override
  public void onStop(Observation.Context context) {
    try {
      ChatResponse response = null;
      String outcome = null;
      if (context instanceof ChatClientObservationContext client && client.getResponse() != null) {
        response = client.getResponse().chatResponse();
      } else if (context instanceof AdvisorObservationContext advisor
          && advisor.getChatClientResponse() != null) {
        response = advisor.getChatClientResponse().chatResponse();
      } else if (context instanceof ChatModelObservationContext model) {
        response = model.getResponse();
      } else if (context instanceof ToolCallingObservationContext) {
        outcome = "Tool result";
      }
      if (response != null) outcome = response.hasToolCalls() ? "Tool request" : "Response";
      store.finish(
          context.get(HANDLE),
          response == null ? null : response.getMetadata().getId(),
          outcome,
          context.getError() == null ? null : context.getError().getClass().getSimpleName());
    } catch (RuntimeException exception) {
      log.debug(
          "Spy could not record an observation stop ({})", exception.getClass().getSimpleName());
    }
  }

  private SpyTraceStore.Start describe(Observation.Context context) {
    if (context instanceof ChatClientObservationContext client) {
      return new SpyTraceStore.Start(
          "client",
          "ChatClient",
          null,
          prompt(client.getRequest().prompt()),
          client.getRequest().prompt().getInstructions().size(),
          client.getAdvisors().stream()
              .map(
                  advisor ->
                      new SpyTraceStore.AdvisorInfo(
                          advisor.getName(),
                          advisor.getOrder(),
                          advisor.getClass().getSimpleName()))
              .toList(),
          null);
    }
    if (context instanceof AdvisorObservationContext advisor) {
      return new SpyTraceStore.Start(
          "advisor",
          advisor.getAdvisorName(),
          advisor.getOrder(),
          prompt(advisor.getChatClientRequest().prompt()),
          advisor.getChatClientRequest().prompt().getInstructions().size(),
          List.of(),
          null);
    }
    if (context instanceof ChatModelObservationContext model) {
      return new SpyTraceStore.Start(
          "model",
          "ChatModel",
          null,
          prompt(model.getRequest()),
          model.getRequest().getInstructions().size(),
          List.of(),
          null);
    }
    var tool = (ToolCallingObservationContext) context;
    return new SpyTraceStore.Start(
        "tool",
        tool.getToolDefinition().name(),
        null,
        "Tool: " + tool.getToolDefinition().name(),
        0,
        List.of(),
        tool.getToolCallId());
  }

  private String prompt(Prompt prompt) {
    var text = prompt.getUserMessage().getText();
    if (text == null) return "(No user prompt)";
    text = text.replaceAll("\\s+", " ").trim();
    return text.substring(0, Math.min(text.length(), 180));
  }
}
