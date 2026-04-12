package com.example.agent.core.json;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public final class JsonUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonUtils() {}

  public static String toJson(Object value) {
    try {
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize to JSON", e);
    }
  }
}
