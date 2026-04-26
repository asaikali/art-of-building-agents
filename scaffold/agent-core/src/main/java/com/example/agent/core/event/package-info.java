/**
 * Per-session, append-only event log. An agent calls {@link
 * com.example.agent.core.session.Session#logEvent} (which delegates to {@link
 * com.example.agent.core.event.EventService}) to record anything worth showing in the inspector's
 * Events panel — tool calls, decision points, downstream status — alongside arbitrary JSON payload
 * data.
 *
 * <p>Each new event is broadcast over SSE via {@link com.example.agent.core.event.EventsController}
 * so the inspector updates live; new subscribers receive the full history first, then the live
 * stream.
 */
package com.example.agent.core.event;
