package com.example.agent.core.chat;

import com.example.agent.core.session.SessionId;
import com.example.agent.core.session.SessionManager;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions/{id}/messages")
public class ChatController {

  private final ChatService chatService;
  private final SessionManager sessionManager;
  private final AgentHandler chatHandler;

  public ChatController(
      ChatService chatService, SessionManager sessionManager, AgentHandler chatHandler) {
    this.chatService = chatService;
    this.sessionManager = sessionManager;
    this.chatHandler = chatHandler;
  }

  @GetMapping
  public List<AgentMessage> getMessages(@PathVariable SessionId id) {
    return chatService.getMessages(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AgentMessage appendMessage(
      @PathVariable SessionId id, @RequestBody AppendMessageRequest request) {
    var message = chatService.appendMessage(id, request.role(), request.text());
    if (request.role() == Role.USER) {
      var session = sessionManager.getSession(id);
      chatHandler.onMessage(session, message);
    }
    return message;
  }

  public record AppendMessageRequest(Role role, String text) {}
}
