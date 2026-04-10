package com.example.businessmealplanner.constraintchecking;

import com.example.agent.core.chat.AgentHandler;
import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

@Component
public class ConstraintCheckingHandler implements AgentHandler {

  private final ChatClient chatClient;
  private final AtomicInteger turnCounter = new AtomicInteger(0);

  public ConstraintCheckingHandler(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  @Override
  public String getName() {
    return "02 - Constraint Checking";
  }

  @Override
  public void onMessage(Session session, AgentMessage message) {
    int turn = turnCounter.incrementAndGet();

    session.logEvent("user-message-received", Map.of("turn", turn, "text", message.text()));
    session.updateState(buildState(turn, "Calling model..."));

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
    return "## Constraint Checking\n\n| Field | Value |\n|-------|-------|\n| Turn | "
        + turn
        + " |\n| Status | "
        + status
        + " |\n| Phase | 02-constraint-checking |\n";
  }
}
