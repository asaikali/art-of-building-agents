package com.example.jarvis.decisionsupport;

import com.example.agent.core.json.JsonUtils;
import com.example.jarvis.planning.PlanningTools;
import com.example.jarvis.requirements.UserRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
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
  private final PlanningTools planningTools;

  public DecisionSupport(ChatClient.Builder chatClientBuilder, PlanningTools planningTools) {
    this.planningTools = planningTools;
    this.chatClient =
        chatClientBuilder
            .defaultSystem(
                """
                You are Jarvis, a warm and professional business meal planning assistant.
                The user has received restaurant recommendations and is now exploring their
                options before making a decision.

                You have access to tools to look up restaurant details and menus when the
                user asks about something not covered in the shortlist.

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
            .defaultTools(planningTools)
            .defaultAdvisors(ToolCallAdvisor.builder().build())
            .build();
  }

  public DecisionSupportResponse ask(
      UserRequirements requirements, String shortlist, String userMessage) {
    log.info("ask | userMessage=\"{}\"", userMessage);

    // Make requirements available to tools that need them
    planningTools.setCurrentRequirements(requirements);

    var response =
        chatClient
            .prompt()
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
