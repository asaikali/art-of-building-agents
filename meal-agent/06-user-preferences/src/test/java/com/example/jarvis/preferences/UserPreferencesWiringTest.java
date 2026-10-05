package com.example.jarvis.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.agent.core.session.AgentSessionService;
import com.example.jarvis.UserPreferencesApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifies that the standalone snapshot wires without making any model calls. */
@SpringBootTest(
    classes = UserPreferencesApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.ai.openai.api-key=unused-for-wiring-test")
class UserPreferencesWiringTest {
  @Autowired AgentSessionService sessions;
  @Autowired UserPreferenceStore preferences;

  @Test
  void freshSessionsRetainUserMemoryWithoutSharingItWithOtherUsers() {
    var first = sessions.createSession("First meal", "alex");
    preferences.remember(
        first.userId(), new UserPreferenceStore.Favourite("canoe", "Canoe", "client dinners"));
    var next = sessions.createSession("Next meal", "alex");
    var ben = sessions.createSession("Ben's meal", "ben");
    assertEquals(1, preferences.favourites(next.userId()).size());
    assertEquals(0, preferences.favourites(ben.userId()).size());
    assertEquals(1, next.getMessages().size());
  }
}
