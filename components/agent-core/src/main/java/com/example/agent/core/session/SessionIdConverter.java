package com.example.agent.core.session;

import org.springframework.core.convert.converter.Converter;

public class SessionIdConverter implements Converter<String, SessionId> {
  @Override
  public SessionId convert(String source) {
    return new SessionId(Integer.parseInt(source));
  }
}
