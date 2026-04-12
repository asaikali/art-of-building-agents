package com.example.agent.core.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public final class JsonUtils {

  private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonUtils() {}

  public static String toJson(Object value) {
    try {
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (JacksonException e) {
      log.error("Failed to serialize to JSON: \n {} \n", value, e);
      throw new IllegalStateException("Failed to serialize to JSON", e);
    }
  }
}
