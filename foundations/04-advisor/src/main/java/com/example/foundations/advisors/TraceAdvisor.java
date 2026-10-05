package com.example.foundations.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

public record TraceAdvisor(String name, int order) implements CallAdvisor {

  private static final Logger logger = LoggerFactory.getLogger(TraceAdvisor.class);

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    logger.info("[{}] before: {} messages", name, request.prompt().getInstructions().size());
    ChatClientResponse response = chain.nextCall(request);
    logger.info(
        "[{}] after: {}",
        name,
        response.chatResponse() != null && response.chatResponse().hasToolCalls()
            ? "tool calls"
            : "final answer");
    return response;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getOrder() {
    return order;
  }
}
