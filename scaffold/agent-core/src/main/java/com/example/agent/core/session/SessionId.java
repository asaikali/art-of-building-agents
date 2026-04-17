package com.example.agent.core.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record SessionId(@JsonValue int value) {
  @JsonCreator
  public SessionId(int value) {
    this.value = value;
  }
}
