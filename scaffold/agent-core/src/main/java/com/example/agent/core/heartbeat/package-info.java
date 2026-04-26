/**
 * Heartbeat SSE channel. Emits a {@link com.example.agent.core.heartbeat.HeartbeatMessage} once a
 * second containing the configured agent name and the current timestamp. The inspector uses it to
 * detect that a backend is up and to display the agent name in the header.
 */
package com.example.agent.core.heartbeat;
