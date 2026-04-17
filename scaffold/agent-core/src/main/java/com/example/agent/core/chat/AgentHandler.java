package com.example.agent.core.chat;

import com.example.agent.core.session.Session;

/** Callback invoked by agent-core when a user message arrives. */
public interface AgentHandler {

  /** The display name for this agent, shown in the inspector header. */
  default String getName() {
    return "Agent";
  }

  /** Optional assistant message shown when a new session is created. */
  default String getInitialAssistantMessage() {
    return null;
  }

  void onMessage(Session session, AgentMessage message);
}
