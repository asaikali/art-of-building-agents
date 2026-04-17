package com.example.agent.core;

import com.example.agent.core.session.SessionIdConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ComponentScan(basePackages = "com.example.agent.core")
public class AgentCoreAutoConfiguration implements WebMvcConfigurer {
  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new SessionIdConverter());
  }
}
