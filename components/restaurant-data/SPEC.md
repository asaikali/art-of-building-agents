# Restaurant Data Spec

## Purpose

This module owns fake restaurant datasets and deterministic lookup services that higher layers can
reuse.

The next dataset to add is restaurant availability.

Its job is simple:

- given a `date`
- given a `time`
- given a `partySize`
- optionally given a `neighborhood`
- return the restaurants that are available

This logic should live in `components/restaurant-data`, not in the Spring AI planning tool.

## Availability Design

The availability model should stay deliberately simple for the workshop.

We do **not** want:

- service windows
- weekly schedules
- fully booked date sets
- random availability generation

We only need to answer:

> Is restaurant `R` available on this exact date, at this exact time, for this exact party size?

## Fake Data Shape

The availability data should live in:

- `components/restaurant-data/src/main/resources/restaurant-data/restaurant-availability.json`

Recommended JSON shape:

```json
{
  "availability": [
    {
      "restaurantId": "canoe",
      "date": "2026-04-20",
      "time": "18:00",
      "maxPartySize": 6
    },
    {
      "restaurantId": "planta-queen",
      "date": "2026-04-20",
      "time": "18:00",
      "maxPartySize": 8
    }
  ]
}
```

Meaning:

- each record represents availability for one restaurant
- on one exact date
- at one exact time
- with a maximum supported party size

Rule:

- a restaurant is available if a matching record exists and `partySize <= maxPartySize`
- otherwise it is not available

## Java Components

The first cut should use two classes.

### `RestaurantAvailabilityData`

Responsibility:

- load `restaurant-availability.json`
- expose deterministic exact-match availability lookup

Suggested shape:

```java
@Component
public class RestaurantAvailabilityData {

  public boolean isAvailable(String restaurantId, LocalDate date, LocalTime time, int partySize) {
    // true if an exact record exists for restaurant/date/time
    // and partySize <= maxPartySize
  }
}
```

Suggested entry record:

```java
public record AvailabilityEntry(
    String restaurantId,
    LocalDate date,
    LocalTime time,
    int maxPartySize) {}
```

### `RestaurantAvailabilityService`

Responsibility:

- validate user input
- optionally filter by neighborhood
- return matching `Restaurant` records

Suggested shape:

```java
@Service
public class RestaurantAvailabilityService {

  public List<Restaurant> findAvailableRestaurants(
      LocalDate date, LocalTime time, int partySize, String neighborhood) {
    // validate inputs
    // filter restaurants by optional neighborhood
    // keep only restaurants available for the given date/time/partySize
    // return deterministic sorted results
  }
}
```

## Validation Rules

`RestaurantAvailabilityService.findAvailableRestaurants(...)` should:

- throw `IllegalArgumentException` if `date` is missing
- throw `IllegalArgumentException` if `time` is missing
- throw `IllegalArgumentException` if `partySize <= 0`

`neighborhood` is optional.

If `neighborhood` is missing or blank, no neighborhood filtering should be applied.

## Matching Rules

Neighborhood filtering should stay simple:

- normalize the input neighborhood to lowercase and trim it
- treat it as a substring match against `restaurant.neighborhood()`

Availability matching should also stay simple:

- exact match on `restaurantId`
- exact match on `date`
- exact match on `time`
- `partySize <= maxPartySize`

If no matching availability record exists, the restaurant should be treated as unavailable.

## Return Value

`findAvailableRestaurants(...)` should return:

- `List<Restaurant>`

Results should be sorted deterministically, for example by restaurant name.

This service is discovery/gating logic only.

It should not:

- run any phase 2 constraint checks
- do scoring
- do ranking beyond deterministic sort order

## Phase Boundary

This deterministic availability service is intended to be used by a higher-level planning tool in
`03-restaurant-planning`.

The tool should be thin and should delegate to this module rather than reimplementing availability
logic.

## Testing

Testing should happen in `components/restaurant-data`.

Recommended tests:

- returns available restaurant when exact date/time matches and party size is within limit
- excludes restaurant when party size exceeds `maxPartySize`
- excludes restaurant when date does not match
- excludes restaurant when time does not match
- filters by neighborhood when provided
- throws for missing `date`
- throws for missing `time`
- throws for invalid `partySize`

The goal is to prove the deterministic availability logic here so phase 3 can call it with
confidence.
