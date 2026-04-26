/**
 * Per-conversation runtime state. {@link com.example.agent.core.session.Session} is the single
 * facade an agent receives in {@link com.example.agent.core.chat.AgentHandler#onMessage}; it gives
 * the agent everything it needs to talk back to the user, log events, publish state, and keep typed
 * working memory across turns.
 *
 * <p>{@link com.example.agent.core.session.SessionManager} owns the in-memory registry of sessions.
 * {@link com.example.agent.core.session.AgentSessionService} adds the platform behaviour around
 * creation (firing a {@code session-started} event, posting any initial assistant message). {@link
 * com.example.agent.core.session.SessionController} exposes session CRUD over REST. {@link
 * com.example.agent.core.session.AgentContext} is the marker interface an agent uses for its own
 * typed per-session context.
 */
package com.example.agent.core.session;
