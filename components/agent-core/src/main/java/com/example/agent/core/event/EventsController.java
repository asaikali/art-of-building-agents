package com.example.agent.core.event;

import com.example.agent.core.session.SessionId;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sessions/{id}/events")
public class EventsController {

  private final EventService eventService;

  public EventsController(EventService eventService) {
    this.eventService = eventService;
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents(@PathVariable SessionId id) {
    return eventService.streamEvents(id);
  }
}
