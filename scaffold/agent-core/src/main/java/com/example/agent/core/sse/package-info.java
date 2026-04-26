/**
 * Generic SSE broadcaster used by the event and state streams. {@link
 * com.example.agent.core.sse.SseBroadcaster} manages per-key (per-session) subscriber lists and
 * supports two patterns: replay history then attach for live updates, and broadcast a single item
 * to all current subscribers.
 *
 * <p>Kept generic and key-agnostic so it can be reused for any future per-session stream without
 * coupling to event or state types.
 */
package com.example.agent.core.sse;
