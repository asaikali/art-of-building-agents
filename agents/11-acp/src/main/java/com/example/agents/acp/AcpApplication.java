package com.example.agents.acp;

import com.agentclientprotocol.sdk.agent.support.AcpAgentSupport;
import com.agentclientprotocol.sdk.agent.transport.StdioAcpAgentTransport;
import com.agentclientprotocol.sdk.agent.transport.WebSocketAcpAgentTransport;
import com.agentclientprotocol.sdk.spec.AcpAgentTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Step 11: Jarvis as an ACP endpoint.
 *
 * <p>Uses Spring Boot for dependency injection (ChatModel from OpenAI auto-configuration), then
 * starts the ACP agent.
 *
 * <p><b>Transports:</b>
 *
 * <ul>
 *   <li>Default: WebSocket on ws://localhost:8083/acp (for testing and demos)
 *   <li>{@code --spring.profiles.active=stdio}: Stdio transport (for IDE integration — IntelliJ,
 *       Zed, VS Code)
 * </ul>
 */
@SpringBootApplication
public class AcpApplication {

  private static final Logger log = LoggerFactory.getLogger(AcpApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(AcpApplication.class, args);
  }

  @Bean
  public RestaurantTools restaurantTools() {
    return new RestaurantTools();
  }

  @Bean
  public JarvisAcpAgent jarvisAcpAgent(ChatModel chatModel, RestaurantTools restaurantTools) {
    return new JarvisAcpAgent(chatModel, restaurantTools);
  }

  @Bean
  public CommandLineRunner startAcpAgent(JarvisAcpAgent jarvisAgent, Environment env) {
    return args -> {
      boolean useStdio =
          env.matchesProfiles("stdio") || java.util.Arrays.asList(args).contains("--stdio");

      AcpAgentTransport transport;
      if (useStdio) {
        transport = new StdioAcpAgentTransport();
        log.info("Starting Jarvis ACP agent on stdio (for IDE integration)");
      } else {
        int port = 8083;
        String path = "/acp";
        transport = new WebSocketAcpAgentTransport(port, path, null);
        log.info("Starting Jarvis ACP agent on ws://localhost:{}{}", port, path);
      }

      AcpAgentSupport agentSupport =
          AcpAgentSupport.create(jarvisAgent).transport(transport).build();

      if (useStdio) {
        agentSupport.run(); // blocks until client disconnects
      } else {
        agentSupport.start(); // non-blocking
        log.info("Jarvis ACP agent started — connect via ws://localhost:8083/acp");
      }
    };
  }
}
