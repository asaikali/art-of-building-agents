package com.example.restaurant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class RestaurantService {

  private final Map<String, Restaurant> restaurantsById;

  public RestaurantService(ObjectMapper mapper) {
    try {
      var resource = new ClassPathResource("restaurant-data/restaurants.json");
      var wrapper =
          mapper.readValue(resource.getInputStream(), new TypeReference<RestaurantList>() {});
      this.restaurantsById =
          wrapper.restaurants().stream()
              .collect(Collectors.toUnmodifiableMap(Restaurant::id, Function.identity()));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load restaurants.json", e);
    }
  }

  public Optional<Restaurant> findById(String id) {
    return Optional.ofNullable(restaurantsById.get(id));
  }

  public List<Restaurant> findAll() {
    return List.copyOf(restaurantsById.values());
  }

  private record RestaurantList(List<Restaurant> restaurants) {}
}
