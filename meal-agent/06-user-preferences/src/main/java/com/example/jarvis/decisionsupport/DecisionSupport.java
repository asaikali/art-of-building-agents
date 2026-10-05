package com.example.jarvis.decisionsupport;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.planning.PlanningTools;
import com.example.jarvis.preferences.UserPreferenceAdvisor;
import com.example.jarvis.preferences.UserPreferenceTools;
import com.example.jarvis.requirements.UserRequirements;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.stereotype.Service;

/**
 * Handles follow-up questions about the restaurant shortlist. The model has the shortlist and
 * confirmed requirements in context, plus tools for deeper lookups (menus, details). Returns a
 * structured response with an action ("answer", "restart", or "selected") and a reply.
 */
@Service
public class DecisionSupport {

  private static final Logger log = LoggerFactory.getLogger(DecisionSupport.class);

  private final ChatClient chatClient;

  public DecisionSupport(
      ChatClient.Builder chatClientBuilder,
      PlanningTools planningTools,
      UserPreferenceAdvisor preferenceAdvisor,
      UserPreferenceTools preferenceTools,
      ToolCallingManager toolCallingManager) {
    this.chatClient =
        chatClientBuilder
            .clone()
            .defaultSystem(
                """
                You are Jarvis, a warm and professional business meal planning assistant.
                The user has received restaurant recommendations and is now exploring their
                options before making a decision.

                You have access to tools to look up restaurant details and menus when the
                user asks about something not covered in the shortlist.

                Use rememberFavourite and forgetFavourite only when the user explicitly
                asks to remember or forget a restaurant. A selection or positive comment
                alone does not request a memory update. Keep action "answer" for memory
                requests. Confirm a memory update only after its tool succeeds.

                For every response, decide which action applies:
                - "answer" — the user is asking a question or comparing options
                - "selected" — the user has picked a restaurant
                - "restart" — the user wants to change their requirements or search again

                Tone:
                - Write like a helpful concierge, warm and specific.
                - When comparing restaurants, focus on what matters for the user's stated
                  purpose (e.g. VIP dinner, casual team lunch).
                - Don't mention internal check names or status codes.
                """)
            .defaultTools(planningTools, preferenceTools)
            .defaultAdvisors(
                preferenceAdvisor,
                ToolCallingAdvisor.builder().toolCallingManager(toolCallingManager).build())
            .build();
  }

  public DecisionSupportResponse ask(
      String userId, UserRequirements requirements, String shortlist, String userMessage) {
    log.info("ask | userMessage=\"{}\"", userMessage);

    var response =
        chatClient
            .prompt()
            .advisors(a -> a.param(UserPreferenceAdvisor.USER_ID, userId))
            .toolContext(
                Map.of(
                    UserPreferenceAdvisor.USER_ID,
                    userId,
                    PlanningTools.REQUIREMENTS,
                    requirements))
            .user(
                u ->
                    u.text(
                            """
                Confirmed requirements:
                {requirements}

                Restaurant shortlist:
                {shortlist}

                User question:
                {userMessage}
                """)
                        .param("requirements", JsonUtils.toJson(requirements))
                        .param("shortlist", shortlist)
                        .param("userMessage", userMessage))
            .call()
            .entity(DecisionSupportResponse.class);

    log.info("ask | action={} | reply=\"{}\"", response.action(), response.reply());
    return response;
  }
}
