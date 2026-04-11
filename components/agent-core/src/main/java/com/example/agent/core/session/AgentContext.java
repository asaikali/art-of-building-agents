package com.example.agent.core.session;

/**
 * Marker interface for an agent-owned context object stored on a {@link Session}.
 *
 * <p>A session already carries shared runtime data such as chat messages, events, and rendered
 * state. {@code AgentContext} is for the agent's own typed working memory: the object it keeps and
 * updates from turn to turn.
 *
 * <p>Different agents can use different context types. This interface stays empty so {@code
 * agent-core} can hold that context without depending on any one agent's model.
 */
public interface AgentContext {}
