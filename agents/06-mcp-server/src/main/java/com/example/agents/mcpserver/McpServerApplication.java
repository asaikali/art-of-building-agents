package com.example.agents.mcpserver;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Step 6a: Restaurant tools exposed as an MCP server.
 *
 * <p>The same {@code @Tool} methods from previous steps, now discoverable by any MCP client. No
 * Spring AI model dependency needed — this is a pure tool server.
 */
@SpringBootApplication
public class McpServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(McpServerApplication.class, args);
  }

  @Bean
  public ToolCallbackProvider restaurantToolCallbacks(RestaurantTools tools) {
    return MethodToolCallbackProvider.builder().toolObjects(tools).build();
  }
}
