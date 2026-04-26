/**
 * The handler-facing seam of agent-core: the {@link com.example.agent.core.chat.AgentHandler}
 * strategy interface that an agent implements to plug into the platform, the {@link
 * com.example.agent.core.chat.AgentMessage} record exchanged with the user, and the REST endpoint
 * ({@link com.example.agent.core.chat.ChatController}) that posts user messages, fires the handler,
 * and reads chat history.
 *
 * <p>An agent that implements {@link com.example.agent.core.chat.AgentHandler} and is registered as
 * a Spring bean will receive a callback for every USER-role message that arrives on the chat
 * endpoint.
 */
package com.example.agent.core.chat;
