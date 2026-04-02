package com.example.agents.memory;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import io.github.markpollack.agentmemory.FileSystemMemoryStore;
import io.github.markpollack.agentmemory.advisor.CompactionMemoryAdvisor;
import io.github.markpollack.workflow.patterns.advisor.AgentLoopAdvisor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

/**
 * Step 7: Memory-enabled agent.
 *
 * <p>Same tools and prompt as Step 04, but now the agent remembers context across conversations.
 * {@link CompactionMemoryAdvisor} injects accumulated knowledge into the system prompt and
 * automatically compacts old memories using a cheap model when the token budget is exceeded.
 *
 * <p><b>What's new vs 04-06:</b>
 *
 * <ul>
 *   <li>{@link FileSystemMemoryStore} — append-only memory stored in {@code .agent-memory/}
 *   <li>{@link CompactionMemoryAdvisor} — Spring AI advisor that injects memory before each call
 *       and appends learnings after each response
 *   <li>LLM-powered compaction — when uncompacted entries exceed the token budget, a cheap model
 *       (gpt-4o-mini) summarizes them into dense patterns
 * </ul>
 *
 * <p><b>Why this matters:</b> Without memory, every conversation starts from zero. With memory, the
 * agent builds up knowledge of user preferences, past bookings, and learned patterns.
 */
@Component
public class MemoryHandler implements AgentHandler {

  private static final Logger log = LoggerFactory.getLogger(MemoryHandler.class);

  private static final String SYSTEM_PROMPT =
      """
      You are Jarvis, a business dinner planning agent for Barcelona.

      Your job: find a restaurant, verify it meets ALL constraints, and book it.

      ## Constraints (check ALL of these)
      - Must be within company expense policy (use checkExpensePolicy tool)
      - Must accommodate dietary requirements (use checkDietaryOptions tool)
      - Must be reachable by the given time

      ## Process
      1. Ask clarifying questions if requirements are unclear
      2. Search for restaurants matching the criteria
      3. For each candidate, verify expense policy AND dietary options
      4. Present only restaurants that pass ALL checks
      5. Book when the user confirms

      ## Tool Selection Rules
      - Use searchRestaurants to find candidates
      - Use checkExpensePolicy BEFORE recommending any restaurant
      - Use checkDietaryOptions if dietary requirements are mentioned
      - Use bookTable only after user confirms the choice

      Never recommend a restaurant without checking expense policy first.
      """;

  private final ChatClient chatClient;
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public MemoryHandler(
      ChatClient.Builder chatClientBuilder,
      org.springframework.ai.chat.model.ChatModel chatModel,
      RestaurantTools restaurantTools) {

    // Persistent memory store — survives across conversations and restarts
    var memoryStore = new FileSystemMemoryStore(Path.of(".agent-memory"));
    log.info("Memory store configured at .agent-memory/");

    // Cheap model for compaction (gpt-4o-mini summarizes old memories)
    var compactionClient =
        ChatClient.builder(chatModel)
            .defaultOptions(OpenAiChatOptions.builder().model("gpt-4o-mini").build())
            .build();

    var memoryAdvisor =
        CompactionMemoryAdvisor.builder(memoryStore)
            .compactionChatClient(compactionClient)
            .memoryTokenBudget(4096)
            .build();

    var agentLoop =
        AgentLoopAdvisor.builder()
            .toolCallingManager(ToolCallingManager.builder().build())
            .maxTurns(15)
            .build();

    this.chatClient =
        chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(restaurantTools)
            .defaultAdvisors(memoryAdvisor, agentLoop)
            .build();
  }

  @Override
  public String getName() {
    return "07 — Memory";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Planning dinner (with memory)..."));

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
    session.updateState(buildState(turn, "Idle — memory in .agent-memory/"));
  }

  private String buildState(int turn, String status) {
    return "## Jarvis — Memory-Enabled Agent\n\n"
        + "| Field | Value |\n|-------|-------|\n"
        + "| Turn | "
        + turn
        + " |\n"
        + "| Status | "
        + status
        + " |\n"
        + "| Tools | search, expensePolicy, dietary, book |\n"
        + "| Loop | AgentLoopAdvisor (max 15 turns) |\n"
        + "| Memory | .agent-memory/ (compaction via gpt-4o-mini) |\n";
  }
}
