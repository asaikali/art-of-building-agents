package com.example.restaurant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class TravelTimeMatrix {

  private final Map<String, Map<String, Integer>> matrix;

  public TravelTimeMatrix(ObjectMapper mapper) {
    try {
      var resource = new ClassPathResource("restaurant-data/travel-time-matrix.json");
      var loaded =
          mapper.readValue(
              resource.getInputStream(), new TypeReference<Map<String, Map<String, Integer>>>() {});
      this.matrix =
          loaded.entrySet().stream()
              .collect(
                  Collectors.toUnmodifiableMap(
                      Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load travel-time-matrix.json", e);
    }
  }

  public Optional<Integer> findBaseMinutes(String fromNeighborhood, String toNeighborhood) {
    if (fromNeighborhood == null || toNeighborhood == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(matrix.get(fromNeighborhood))
        .map(destinations -> destinations.get(toNeighborhood));
  }

  public Map<String, Map<String, Integer>> asMap() {
    return matrix;
  }
}
