# 03 Tool Calling

From the repository root, run the app with `OPENAI_API_KEY` set in your environment:

```shell
./mvnw -pl foundations/03-tool-calling -am install -DskipTests
./mvnw -f foundations/03-tool-calling/pom.xml spring-boot:run
```

Stop any other sample using port 8080. Open [Spy](http://localhost:8080/) to
inspect the model requests and responses.

The samples in
[ToolCallingController](src/main/java/com/example/foundations/tools/ToolCallingController.java)
start with one weather lookup, then use multiple lookups to give packing
advice for a trip to Toronto and Paris.

## Define the weather tool

The [WeatherService](src/main/java/com/example/foundations/tools/WeatherService.java)
exposes a regular Java method with `@Tool`. Its parameter has a description
for the model:

```java
@Tool(description = "Get the current weather for a city, including temperature in Celsius, conditions, humidity, and wind in km/h")
public WeatherResponse getCurrentWeather(
    @ToolParam(description = "City name, for example Toronto or Paris") String city) {
  Random random = new Random();
  String condition = CONDITIONS[random.nextInt(CONDITIONS.length)];
  double temperature = switch (condition) {
    case "Snowy" -> -10 + 10 * random.nextDouble();
    case "Rainy" -> 5 + 15 * random.nextDouble();
    default -> 10 + 20 * random.nextDouble();
  };
  int humidity = switch (condition) {
    case "Rainy", "Snowy" -> 70 + random.nextInt(31);
    default -> 30 + random.nextInt(51);
  };
  return new WeatherResponse(
      city,
      roundToOneDecimalPlace(temperature),
      condition,
      humidity,
      roundToOneDecimalPlace(20 * random.nextDouble()),
      WIND_DIRECTIONS[random.nextInt(WIND_DIRECTIONS.length)],
      LocalDateTime.now());
}
```

The tool returns a record:

```java
public record WeatherResponse(
    String city,
    double temperature,
    String weatherCondition,
    int humidity,
    double windSpeed,
    String windDirection,
    LocalDateTime timeStamp) {}
```

The service generates simulated weather on every tool call, adapting the
zero-to-hero generator so temperature and humidity fit the chosen condition:

| Condition | Temperature | Humidity |
| --- | --- | --- |
| Snowy | -10 to 0°C | 70 to 100% |
| Rainy | 5 to 20°C | 70 to 100% |
| Sunny, cloudy, or partly cloudy | 10 to 30°C | 30 to 80% |

Wind speed ranges from 0 to 20 km/h, with a random wind direction and the
current timestamp. Temperature and wind speed are rounded to one decimal
place. It needs no weather API account. Repeating a request can produce
different weather and packing advice.

## Creating the ChatClient

As in the other foundations, the controller builds a client from Spring
Boot's configured builder and injects the weather service:

```java
public ToolCallingController(ChatClient.Builder builder, WeatherService weatherService) {
  this.chatClient = builder.build();
  this.weatherService = weatherService;
}
```

Passing `.tools(weatherService)` makes its annotated methods available to the
model for that request. Spring AI generates the tool definition and input
schema from the method and its annotations.

## 1. One city, one tool call

```java
@GetMapping("/weather")
public String weather(@RequestParam(defaultValue = "Toronto") String city) {
  return chatClient
      .prompt()
      .tools(weatherService)
      .user(u -> u.text("What is the current weather in {city}?").param("city", city))
      .call()
      .content();
}
```

```bash
http GET :8080/tools/weather
```

**What to observe:** in Spy, the first request includes `getCurrentWeather`
in `tools`, with a `city` input. The model responds with a `tool_calls` entry
for Toronto. Spring AI invokes the Java method and sends its result back in
a message with role `tool`. The model then writes the weather answer.

One tool call usually means two model HTTP exchanges: one to request the
lookup and one to turn its result into the final answer. The controller
contains only one `.call()`; Spring AI handles the tool loop.

## 2. Multiple cities, multiple tool calls

Ask for packing advice for both cities:

```java
@GetMapping("/pack")
public String pack(@RequestParam(defaultValue = "Toronto and Paris") String cities) {
  return chatClient
      .prompt()
      .tools(weatherService)
      .user(
          u -> u.text("""
              I am traveling to {cities}. What clothes should I pack.
              """)
              .param("cities", cities))
      .call()
      .content();
}
```

```bash
http GET :8080/tools/pack
```

**What to observe:** the model needs weather for Toronto and Paris. Look
for two calls to `getCurrentWeather`, each with a different `city` argument,
and their corresponding tool results. Compare the packing advice with each
city's generated temperature, conditions, and wind. These values are generated
afresh on each tool call.

The model may request both lookups in one response or across successive
turns. Count the entries in `tool_calls` separately from the HTTP exchanges
in Spy. Both lookups use the same annotated Java method; the controller
does not split the cities or loop over them.

To compare with packing for just one city, change only the parameter:

```bash
http GET :8080/tools/pack cities==Toronto
```

## Takeaways

- `@Tool` exposes a method; `@ToolParam` describes its inputs.
- `.tools(weatherService)` makes the tool available for a request.
- The model chooses the tool calls and arguments; Spring AI executes the methods.
- One user request can produce multiple tool calls before the final answer.

See [Spring AI's tool calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html).
