package com.example.agent.core.state;

import com.example.agent.core.session.SessionId;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sessions/{id}/state")
public class StateController {

  private final StateService stateService;

  public StateController(StateService stateService) {
    this.stateService = stateService;
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamState(@PathVariable SessionId id) {
    return stateService.streamState(id);
  }

  @GetMapping("/rev/{rev}")
  public AgentStateRevision getStateRevision(@PathVariable SessionId id, @PathVariable long rev) {
    return stateService.getStateRevision(id, rev);
  }
}
