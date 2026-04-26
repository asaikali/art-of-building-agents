/**
 * Versioned, user-visible agent state. An agent calls {@link
 * com.example.agent.core.session.Session#updateState} (delegating to {@link
 * com.example.agent.core.state.StateService}) to publish a markdown snapshot of whatever it wants
 * the inspector's State panel to show — captured requirements, current plan, decision shortlist,
 * anything.
 *
 * <p>Each call creates a new {@link com.example.agent.core.state.AgentStateRevision} with a
 * monotonic revision number. Subscribers stream live updates via {@link
 * com.example.agent.core.state.StateController}, and any specific revision can be fetched by number
 * for revision browsing.
 */
package com.example.agent.core.state;
