/**
 * Web-layer plumbing not specific to any one feature. {@link
 * com.example.agent.core.web.AgentCoreExceptionHandler} translates exceptions thrown by controllers
 * into consistent JSON error responses. {@link com.example.agent.core.web.SpaForwardController}
 * forwards client-side routes (e.g. {@code /sessions/...}) to {@code /index.html} so the SPA
 * router can handle them.
 */
package com.example.agent.core.web;
