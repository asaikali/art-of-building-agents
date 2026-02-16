package com.example.agent.core.heartbeat;

import com.example.agent.core.chat.AgentHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/heartbeat")
public class HeartbeatController {

  private final String agentName;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public HeartbeatController(AgentHandler chatHandler) {
    this.agentName = chatHandler.getName();
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream() {
    var emitter = new SseEmitter(Long.MAX_VALUE);

    var future =
        scheduler.scheduleAtFixedRate(
            () -> {
              try {
                emitter.send(
                    SseEmitter.event().data(new HeartbeatMessage(agentName, Instant.now())));
              } catch (IOException e) {
                emitter.completeWithError(e);
              }
            },
            0,
            1,
            TimeUnit.SECONDS);

    emitter.onCompletion(() -> future.cancel(false));
    emitter.onTimeout(() -> future.cancel(false));
    emitter.onError(e -> future.cancel(false));

    return emitter;
  }
}
