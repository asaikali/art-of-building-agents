package com.example.agents.acp;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentclientprotocol.sdk.agent.support.AcpAgentSupport;
import com.agentclientprotocol.sdk.client.AcpAsyncClient;
import com.agentclientprotocol.sdk.client.AcpClient;
import com.agentclientprotocol.sdk.spec.AcpSchema.InitializeRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.NewSessionRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.PromptRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.PromptResponse;
import com.agentclientprotocol.sdk.spec.AcpSchema.TextContent;
import com.agentclientprotocol.sdk.test.InMemoryTransportPair;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;

/** Integration test for JarvisAcpAgent using ACP in-memory transport. Requires OPENAI_API_KEY. */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class JarvisAcpAgentTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(120);

  @Test
  void jarvisRespondsViaAcp() throws Exception {
    InMemoryTransportPair transportPair = InMemoryTransportPair.create();

    // Create real ChatModel and tools
    OpenAiApi api = OpenAiApi.builder().apiKey(System.getenv("OPENAI_API_KEY")).build();
    ChatModel chatModel = OpenAiChatModel.builder().openAiApi(api).build();
    RestaurantTools restaurantTools = new RestaurantTools();

    JarvisAcpAgent jarvisAgent = new JarvisAcpAgent(chatModel, restaurantTools);

    AcpAgentSupport agentSupport =
        AcpAgentSupport.create(jarvisAgent)
            .transport(transportPair.agentTransport())
            .requestTimeout(TIMEOUT)
            .build();

    agentSupport.start();
    Thread.sleep(50);

    AcpAsyncClient client =
        AcpClient.async(transportPair.clientTransport()).requestTimeout(TIMEOUT).build();

    // ACP lifecycle: initialize → newSession → prompt
    client.initialize(new InitializeRequest(1, null)).block(TIMEOUT);
    var session = client.newSession(new NewSessionRequest("/workspace", List.of())).block(TIMEOUT);
    assertThat(session.sessionId()).isNotNull();

    PromptResponse response =
        client
            .prompt(
                new PromptRequest(
                    session.sessionId(),
                    List.of(
                        new TextContent(
                            "Find a restaurant in Eixample for 4 people, budget 30 EUR per person, one vegetarian"))))
            .block(TIMEOUT);

    assertThat(response).isNotNull();
    assertThat(response.stopReason()).isNotNull();

    client.closeGracefully().block(TIMEOUT);
    agentSupport.close();
  }
}
