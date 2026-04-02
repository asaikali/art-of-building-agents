package com.example.agents.workflow;

import io.github.markpollack.workflow.flows.Step;
import io.github.markpollack.workflow.flows.workflow.Workflow;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Three-step "sandwich" workflow: deterministic context gathering, AI recommendation, deterministic
 * validation. The LLM does ONLY what it's good at (reasoning). Filtering and validation are
 * zero-token, guaranteed correct.
 */
public class DinnerPlanningWorkflow {

  private static final double CORPORATE_EXPENSE_LIMIT = 50.0;

  private static final List<Map<String, Object>> RESTAURANTS =
      List.of(
          Map.of(
              "name", "Can Culleretes",
              "neighborhood", "Gothic Quarter",
              "cuisine", "Catalan",
              "pricePerPerson", 35,
              "vegetarianOptions", true,
              "noiseLevel", "moderate"),
          Map.of(
              "name", "Cervecería Catalana",
              "neighborhood", "Eixample",
              "cuisine", "Tapas",
              "pricePerPerson", 28,
              "vegetarianOptions", true,
              "noiseLevel", "lively"),
          Map.of(
              "name", "El Nacional",
              "neighborhood", "Passeig de Gràcia",
              "cuisine", "Mediterranean",
              "pricePerPerson", 55,
              "vegetarianOptions", true,
              "noiseLevel", "moderate"),
          Map.of(
              "name", "Tickets Bar",
              "neighborhood", "Paral·lel",
              "cuisine", "Modern Spanish",
              "pricePerPerson", 75,
              "vegetarianOptions", false,
              "noiseLevel", "lively"),
          Map.of(
              "name", "Teresa Carles",
              "neighborhood", "Eixample",
              "cuisine", "Vegetarian",
              "pricePerPerson", 22,
              "vegetarianOptions", true,
              "noiseLevel", "quiet"));

  private final ChatClient chatClient;

  public DinnerPlanningWorkflow(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  /** Runs the three-step workflow and returns the final result. */
  public DinnerResult run(DinnerRequest request) {
    // Step 1: Deterministic — gather context and filter candidates
    Step<DinnerRequest, GatherResult> gatherContext =
        Step.named(
            "gather-context",
            (ctx, req) -> {
              List<Map<String, Object>> candidates =
                  RESTAURANTS.stream()
                      .filter(
                          r -> {
                            boolean matchNeighborhood =
                                req.neighborhood() == null
                                    || req.neighborhood().isEmpty()
                                    || r.get("neighborhood")
                                        .toString()
                                        .toLowerCase()
                                        .contains(req.neighborhood().toLowerCase());
                            boolean matchCuisine =
                                req.cuisine() == null
                                    || req.cuisine().isEmpty()
                                    || r.get("cuisine")
                                        .toString()
                                        .toLowerCase()
                                        .contains(req.cuisine().toLowerCase());
                            boolean withinBudget =
                                ((int) r.get("pricePerPerson")) <= req.budgetPerPerson();
                            boolean meetsDietary =
                                req.dietary() == null
                                    || req.dietary().isEmpty()
                                    || (req.dietary().equalsIgnoreCase("vegetarian")
                                        && (boolean) r.get("vegetarianOptions"));
                            return matchNeighborhood
                                && matchCuisine
                                && withinBudget
                                && meetsDietary;
                          })
                      .toList();
              return new GatherResult(candidates, req);
            });

    // Step 2: AI — recommend from filtered candidates
    Step<GatherResult, RecommendResult> recommend =
        Step.named(
            "recommend",
            (ctx, gathered) -> {
              if (gathered.candidates().isEmpty()) {
                return new RecommendResult(
                    gathered, "No restaurants match all constraints. Try relaxing the budget.");
              }
              String candidateList =
                  gathered.candidates().stream()
                      .map(
                          r ->
                              String.format(
                                  "- %s (%s, %s cuisine, %s%d/person, vegetarian: %s, noise: %s)",
                                  r.get("name"),
                                  r.get("neighborhood"),
                                  r.get("cuisine"),
                                  "\u20ac",
                                  r.get("pricePerPerson"),
                                  r.get("vegetarianOptions"),
                                  r.get("noiseLevel")))
                      .collect(Collectors.joining("\n"));

              String prompt =
                  String.format(
                      """
                      You are a restaurant recommendation assistant for Barcelona business dinners.

                      Requirements:
                      - Party size: %d
                      - Budget: %s%.0f per person
                      - Dietary needs: %s

                      Candidate restaurants (already filtered to match constraints):
                      %s

                      Pick the BEST option and explain why it fits. Be concise (2-3 sentences).
                      Start with the restaurant name.""",
                      gathered.request().partySize(),
                      "\u20ac",
                      gathered.request().budgetPerPerson(),
                      gathered.request().dietary() != null ? gathered.request().dietary() : "none",
                      candidateList);

              String reply = chatClient.prompt().user(prompt).call().content();
              return new RecommendResult(gathered, reply);
            });

    // Step 3: Deterministic — validate the recommendation
    Step<RecommendResult, DinnerResult> validate =
        Step.named(
            "validate",
            (ctx, rec) -> {
              List<Map<String, Object>> candidates = rec.gathered().candidates();
              String reply = rec.recommendation();

              // Check: does the recommendation reference a candidate?
              Map<String, Object> matched =
                  candidates.stream()
                      .filter(
                          r -> reply.toLowerCase().contains(r.get("name").toString().toLowerCase()))
                      .findFirst()
                      .orElse(null);

              if (matched == null) {
                return new DinnerResult(
                    candidates,
                    reply,
                    false,
                    "Validation failed: recommended restaurant not in candidate list");
              }

              int price = (int) matched.get("pricePerPerson");
              if (price > rec.gathered().request().budgetPerPerson()) {
                return new DinnerResult(
                    candidates,
                    reply,
                    false,
                    String.format(
                        "Validation failed: %s costs %s%d/person, exceeds budget of %s%.0f",
                        matched.get("name"),
                        "\u20ac",
                        price,
                        "\u20ac",
                        rec.gathered().request().budgetPerPerson()));
              }

              return new DinnerResult(
                  candidates,
                  reply,
                  true,
                  String.format(
                      "Validated: %s at %s%d/person within budget",
                      matched.get("name"), "\u20ac", price));
            });

    // Wire the 3-step workflow
    return Workflow.<DinnerRequest, DinnerResult>define("dinner-planning")
        .step(gatherContext)
        .step(recommend)
        .step(validate)
        .run(request);
  }

  /** Intermediate result after gathering context. */
  record GatherResult(List<Map<String, Object>> candidates, DinnerRequest request) {}

  /** Intermediate result after AI recommendation. */
  record RecommendResult(GatherResult gathered, String recommendation) {}
}
