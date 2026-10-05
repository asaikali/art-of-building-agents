package com.example.jarvis.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.agent.core.chat.AgentMessage;
import com.example.agent.core.chat.Role;
import com.example.agent.core.session.Session;
import com.example.jarvis.requirements.UserRequirements;
import com.example.jarvis.requirements.alignment.AlignmentStatus;
import com.example.jarvis.requirements.alignment.RequirementsAligner;
import com.example.jarvis.search.RestaurantSearcher;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class JarvisAgentHandlerTest {

  private final RequirementsAligner aligner = mock(RequirementsAligner.class);
  private final RestaurantSearcher searcher = mock(RestaurantSearcher.class);
  private final Session session = mock(Session.class);
  private final JarvisAgentContext context = new JarvisAgentContext();
  private final UserRequirements requirements = new UserRequirements();
  private final JarvisAgentHandler handler = new JarvisAgentHandler(aligner, searcher);

  @BeforeEach
  void setUp() {
    requirements.getMeal().setDate(LocalDate.of(2026, 10, 20));
    requirements.getMeal().setTime(LocalTime.of(18, 0));
    requirements.getMeal().setPartySize(2);
    when(session.getOrCreateContext(eq(JarvisAgentContext.class), any())).thenReturn(context);
  }

  @ParameterizedTest
  @EnumSource(
      value = AlignmentStatus.class,
      names = {"GATHERING_REQUIREMENTS", "CONFIRMING_REQUIREMENTS"})
  void doesNotSearchBeforeTheUserConfirms(AlignmentStatus status) {
    when(aligner.processMessage(any(), any(), anyString()))
        .thenReturn(
            new RequirementsAligner.Result(requirements, List.of(), status, "Please confirm."));

    handler.onMessage(session, new AgentMessage(Instant.now(), Role.USER, "Dinner for two."));

    verifyNoInteractions(searcher);
    assertThat(context.getAlignmentStatus()).isEqualTo(status);
  }

  @Test
  void searchesWithConfirmedRequirementsAndPublishesTheCandidates() {
    context.setAlignmentStatus(AlignmentStatus.CONFIRMING_REQUIREMENTS);
    when(aligner.processMessage(any(), any(), anyString()))
        .thenReturn(
            new RequirementsAligner.Result(
                requirements, List.of(), AlignmentStatus.REQUIREMENTS_CONFIRMED, "Confirmed."));
    when(searcher.search(requirements))
        .thenReturn("Canoe is available; other requirements need checking.");

    handler.onMessage(session, new AgentMessage(Instant.now(), Role.USER, "Yes."));

    verify(searcher).search(requirements);
    verify(session).reply("Canoe is available; other requirements need checking.");
    verify(session).logEvent("search-started", java.util.Map.of());
    verify(session).logEvent("search-completed", java.util.Map.of());
    assertThat(context.getUserRequirements()).isSameAs(requirements);
    assertThat(context.getAlignmentStatus()).isEqualTo(AlignmentStatus.GATHERING_REQUIREMENTS);
  }
}
