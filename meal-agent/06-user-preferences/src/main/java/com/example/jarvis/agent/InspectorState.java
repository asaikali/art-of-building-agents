package com.example.jarvis.agent;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.preferences.UserPreferenceStore.Favourite;
import java.util.List;

/** Formats the agent context as Markdown for the Inspector's state panel. */
final class InspectorState {

  private InspectorState() {}

  static String render(
      JarvisAgentContext context,
      String status,
      String planningResult,
      String userId,
      List<Favourite> favourites) {
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

    state +=
        "\n## User Preferences ("
            + userId
            + ")\n\n```json\n"
            + JsonUtils.toJson(favourites)
            + "\n```\n";

    if (planningResult != null) {
      state += "\n## Planning Result\n\n" + planningResult;
    }

    return state;
  }
}
