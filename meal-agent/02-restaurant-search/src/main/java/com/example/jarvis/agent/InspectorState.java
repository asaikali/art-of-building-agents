package com.example.jarvis.agent;

import com.example.agent.core.json.JsonUtils;

/** Formats the agent context as Markdown for the Inspector's state panel. */
final class InspectorState {

  private InspectorState() {}

  static String render(JarvisAgentContext context, String status, String searchResult) {
    var state =
        """
        # Agent Context

        ## Requirements
        ```json
        %s
        ```

        ## Status
        %s
        """
            .formatted(JsonUtils.toJson(context.getUserRequirements()), status);

    if (searchResult != null) {
      state += "\n## Search Result\n\n" + searchResult;
    }

    return state;
  }
}
