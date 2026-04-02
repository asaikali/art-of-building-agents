package com.example.agents.subagent;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import io.github.markpollack.workflow.patterns.advisor.AgentLoopAdvisor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.stereotype.Component;

/**
 * Step 9: Sub-agent delegation.
 *
 * <p>Jarvis no longer calls restaurant tools directly. Instead, it delegates research to a
 * specialized sub-agent via {@link ResearcherTool}. The sub-agent runs in its own context with its
 * own system prompt and tools, then returns findings to Jarvis who presents them to the user.
 *
 * <p><b>What's new vs 04-08:</b>
 *
 * <ul>
 *   <li>{@link ResearcherTool} — creates a fresh ChatClient with restaurant tools and a
 *       research-optimized prompt
 *   <li>Jarvis delegates via {@code delegateToResearcher} tool — the sub-agent does the search,
 *       expense, and dietary checks
 *   <li>Each sub-agent call gets its own context window — no cross-contamination
 * </ul>
 *
 * <p><b>Why this matters:</b> Sub-agents let you decompose complex tasks. Each agent is optimized
 * for its role: Jarvis coordinates and communicates with the user; the researcher is optimized for
 * thorough search and verification.
 */
@Component
public class SubagentHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(SubagentHandler.class);

  private static final String SYSTEM_PROMPT =
      """
      You are Jarvis, a business dinner planning coordinator for Barcelona.

      You do NOT search for restaurants yourself. Instead, you delegate research to
      a specialized restaurant researcher sub-agent using the delegateToResearcher tool.

      ## Process
      1. Understand the user's requirements (neighborhood, budget, dietary needs, party size)
      2. Delegate research to the sub-agent with clear requirements
      3. Present the sub-agent's findings to the user in a friendly format
      4. Help the user choose and handle any follow-up questions

      ## Rules
      - ALWAYS use delegateToResearcher for restaurant search and verification
      - Include ALL known requirements when delegating (don't lose context)
      - Present results clearly with key details highlighted
      """;

  private final ChatClient chatClient;
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public SubagentHandler(
      ChatClient.Builder chatClientBuilder, ChatModel chatModel, RestaurantTools restaurantTools) {

    // The researcher sub-agent — Jarvis's only tool
    var researcherTool = new ResearcherTool(chatModel, restaurantTools);

    var agentLoop =
        AgentLoopAdvisor.builder()
            .toolCallingManager(ToolCallingManager.builder().build())
            .maxTurns(15)
            .build();

    this.chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(researcherTool)
            .defaultAdvisors(agentLoop)
            .build();
  }

  @Override
  public String getName() {
    return "09 — Sub-Agent";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Coordinating (delegating research to sub-agent)..."));

    List<Message> history =
        session.getMessages().stream()
            .map(
                m ->
                    (Message)
                        (m.role() == Role.USER
                            ? new UserMessage(m.text())
                            : new AssistantMessage(m.text())))
            .toList();

    String reply = chatClient.prompt().messages(history).call().content();

    session.logEvent("assistant-reply-sent", Map.of("turn", turn, "reply", reply));
    session.appendMessage(Role.ASSISTANT, reply);
    session.updateState(buildState(turn, "Idle"));
  }

  private String buildState(int turn, String status) {
    return "## Jarvis — Sub-Agent Coordinator\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | "
        + status
        + " |\n"
        + "| Jarvis Tools | delegateToResearcher |\n"
        + "| Sub-Agent Tools | search, expensePolicy, dietary, book |\n"
        + "| Loop | AgentLoopAdvisor (max 15 turns) |\n";
  }
}
