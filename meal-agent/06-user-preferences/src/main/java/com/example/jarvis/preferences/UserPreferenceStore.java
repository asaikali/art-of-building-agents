package com.example.jarvis.preferences;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** User memory lives beyond a meal session, but only for this process's lifetime. */
@Service
public class UserPreferenceStore {
  public record Favourite(String restaurantId, String restaurantName, String occasion) {}

  private final Map<String, Map<String, Favourite>> users = new ConcurrentHashMap<>();

  public List<Favourite> favourites(String userId) {
    return users.getOrDefault(userId, Map.of()).values().stream()
        .sorted(java.util.Comparator.comparing(Favourite::restaurantId))
        .toList();
  }

  public void remember(String userId, Favourite favourite) {
    users
        .computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
        .put(favourite.restaurantId(), favourite);
  }

  public boolean forget(String userId, String restaurantId) {
    var favourites = users.get(userId);
    return favourites != null && favourites.remove(restaurantId) != null;
  }
}
