package com.example.agents.acp;

import com.agentclientprotocol.sdk.agent.SyncPromptContext;
import com.agentclientprotocol.sdk.annotation.Initialize;
import com.agentclientprotocol.sdk.annotation.NewSession;
import com.agentclientprotocol.sdk.annotation.Prompt;
import com.agentclientprotocol.sdk.spec.AcpSchema.InitializeResponse;
import com.agentclientprotocol.sdk.spec.AcpSchema.NewSessionRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.NewSessionResponse;
import com.agentclientprotocol.sdk.spec.AcpSchema.PromptRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.PromptResponse;
import com.agentclientprotocol.sdk.spec.AcpSchema.TextContent;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Jarvis as an ACP (Agent Communication Protocol) agent.
 *
 * <p>This is the same Jarvis restaurant agent from previous steps, but exposed via ACP instead of
 * the Inspector web UI. ACP is the protocol IDEs (Zed, JetBrains, VS Code) use to talk to agents.
 *
 * <p><b>Key annotations:</b>
 *
 * <ul>
 *   <li>{@link com.agentclientprotocol.sdk.annotation.AcpAgent @AcpAgent} — marks this class as an
 *       ACP agent
 *   <li>{@link Initialize @Initialize} — handles the ACP initialize handshake
 *   <li>{@link NewSession @NewSession} — creates a new conversation session
 *   <li>{@link Prompt @Prompt} — handles user prompts (the main agent loop)
 * </ul>
 */
@com.agentclientprotocol.sdk.annotation.AcpAgent(name = "jarvis", version = "1.0.0")
public class JarvisAcpAgent {

  private static final Logger log = LoggerFactory.getLogger(JarvisAcpAgent.class);

  private static final String SYSTEM_PROMPT =
      """
      You are Jarvis, a business dinner planning assistant for Barcelona.

      ## Available Tools
      - searchRestaurants — find restaurants by neighborhood or cuisine
      - checkExpensePolicy — verify price is within corporate limits
      - checkDietaryOptions — check dietary requirements
      - bookTable — make a reservation

      ## Process
      1. Search for restaurants matching criteria
      2. Check expense policy for candidates
      3. Check dietary options if needed
      4. Present qualifying restaurants
      5. Book when the user decides

      ## Rules
      - ALWAYS check expense policy before recommending
      - Only recommend restaurants that pass ALL checks
      - Be concise and helpful
      """;

  private final ChatModel chatModel;
  private final RestaurantTools restaurantTools;
  private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();

  public JarvisAcpAgent(ChatModel chatModel, RestaurantTools restaurantTools) {
    this.chatModel = chatModel;
    this.restaurantTools = restaurantTools;
  }

  @Initialize
  InitializeResponse init() {
    log.info("ACP agent initialized");
    return InitializeResponse.ok();
  }

  @NewSession
  NewSessionResponse newSession(NewSessionRequest req) {
    String sessionId = UUID.randomUUID().toString();
    sessions.put(sessionId, new java.util.ArrayList<>());
    log.info("New session: {} (cwd: {})", sessionId, req.cwd());
    return new NewSessionResponse(sessionId, null, null);
  }

  @Prompt
  PromptResponse prompt(PromptRequest req, SyncPromptContext ctx) {
    String userText = extractText(req);
    log.info("Prompt [session {}]: {}", ctx.getSessionId(), userText);

    ctx.sendMessage("Searching for restaurants...");

    ChatClient chatClient =
        ChatClient.builder(chatModel)
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(restaurantTools)
            .defaultAdvisors(ToolCallAdvisor.builder().build())
            .build();

    // Build history from session
    List<Message> history = sessions.getOrDefault(ctx.getSessionId(), new java.util.ArrayList<>());
    history.add(new UserMessage(userText));

    String reply = chatClient.prompt().messages(history).call().content();

    history.add(new AssistantMessage(reply));
    sessions.put(ctx.getSessionId(), history);

    log.info("Reply: {}", reply);
    return PromptResponse.text(reply);
  }

  private static String extractText(PromptRequest req) {
    if (req.prompt() != null && !req.prompt().isEmpty()) {
      var content = req.prompt().get(0);
      if (content instanceof TextContent tc) {
        return tc.text();
      }
    }
    return "";
  }
}
