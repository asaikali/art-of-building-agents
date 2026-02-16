package com.example.agent.core.web;

import com.example.agent.core.session.SessionNotFoundException;
import com.example.agent.core.state.RevisionNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AgentCoreExceptionHandler {

  @ExceptionHandler(SessionNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, String> handleSessionNotFound(SessionNotFoundException ex) {
    return Map.of("error", ex.getMessage());
  }

  @ExceptionHandler(RevisionNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, String> handleRevisionNotFound(RevisionNotFoundException ex) {
    return Map.of("error", ex.getMessage());
  }
}
