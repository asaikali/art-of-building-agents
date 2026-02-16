package com.example.agent.core.session;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

  private final SessionManager sessionManager;
  private final SessionMetaAssembler metaAssembler;

  public SessionController(SessionManager sessionManager, SessionMetaAssembler metaAssembler) {
    this.sessionManager = sessionManager;
    this.metaAssembler = metaAssembler;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AgentSessionMeta createSession(@RequestBody CreateSessionRequest request) {
    Session session = sessionManager.createSession(request.agentName(), request.title());
    return metaAssembler.toMeta(session.id());
  }

  @GetMapping
  public List<AgentSessionMeta> listSessions() {
    return metaAssembler.listMetas();
  }

  @GetMapping("/{id}/meta")
  public AgentSessionMeta getSessionMeta(@PathVariable SessionId id) {
    return metaAssembler.toMeta(id);
  }

  public record CreateSessionRequest(String agentName, String title) {}
}
