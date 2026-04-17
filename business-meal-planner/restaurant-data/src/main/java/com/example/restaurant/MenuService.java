package com.example.restaurant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class MenuService {

  private final Map<String, RestaurantMenu> menusById;

  public MenuService(ObjectMapper mapper) {
    try {
      var resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath:restaurant-data/menus/*.json");
      this.menusById =
          java.util.Arrays.stream(resources)
              .map(
                  resource -> {
                    try {
                      return mapper.readValue(resource.getInputStream(), RestaurantMenu.class);
                    } catch (IOException e) {
                      throw new UncheckedIOException(
                          "Failed to load menu: " + resource.getFilename(), e);
                    }
                  })
              .collect(
                  Collectors.toUnmodifiableMap(RestaurantMenu::restaurantId, Function.identity()));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to scan menu resources", e);
    }
  }

  public Optional<RestaurantMenu> findById(String restaurantId) {
    return Optional.ofNullable(menusById.get(restaurantId));
  }

  public List<RestaurantMenu> findAll() {
    return List.copyOf(menusById.values());
  }
}
