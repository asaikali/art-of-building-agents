package com.example.agent.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.example.agent.core.chat.ChatService;
import com.example.agent.core.event.EventService;
import com.example.agent.core.state.StateService;
import org.junit.jupiter.api.Test;

class SessionOwnershipTest {
  @Test
  void sessionMetadataRetainsItsOwnerWhenOtherUsersCreateChats() {
    var events = mock(EventService.class);
    var state = mock(StateService.class);
    var manager = new SessionManager(mock(ChatService.class), events, state);
    var alex = manager.createSession("Jarvis", "Client dinner", "alex");
    var ben = manager.createSession("Jarvis", "Team lunch", "ben");
    var metadata = new SessionMetaAssembler(manager, events, state);

    assertEquals("alex", manager.getSession(alex.id()).userId());
    assertEquals("alex", metadata.toMeta(alex.id()).userId());
    assertEquals("ben", metadata.toMeta(ben.id()).userId());
    assertEquals("alex", manager.createSession("Jarvis", "Legacy chat").userId());
  }
}
