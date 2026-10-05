package com.example.jarvis.agent;

import com.example.agent.core.json.JsonUtils;

/** Formats the agent context as Markdown for the Inspector's state panel. */
final class InspectorState {

  private InspectorState() {}

  static String render(JarvisAgentContext context) {
    return """
        # Agent Context

        ## Requirements
        ```json
        %s
        ```

        ## Status
        %s
        """
        .formatted(
            JsonUtils.toJson(context.getUserRequirements()), context.getAlignmentStatus().label());
  }
}
