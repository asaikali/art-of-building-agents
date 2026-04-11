package com.example.jarvis.requirements.alignment;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TurnClassifier {

  public boolean isAffirmative(String text) {
    return switch (normalize(text)) {
      case "yes",
          "y",
          "correct",
          "looks good",
          "look good",
          "exactly",
          "that's right",
          "thats right",
          "right",
          "sounds good",
          "confirmed" ->
          true;
      default -> false;
    };
  }

  public boolean isOpener(String text) {
    return switch (normalize(text)) {
      case "hi", "hello", "hey", "can you help", "help", "what do you need" -> true;
      default -> false;
    };
  }

  public boolean isUncertain(String text) {
    return switch (normalize(text)) {
      case "maybe",
          "not sure",
          "unsure",
          "i don't know",
          "i dont know",
          "don't know",
          "dont know",
          "whatever",
          "you decide",
          "anything" ->
          true;
      default -> false;
    };
  }

  private String normalize(String text) {
    if (text == null) {
      return "";
    }
    String normalized = text.toLowerCase(Locale.ROOT).trim();
    normalized = normalized.replaceAll("[.!?]+$", "");
    normalized = normalized.replaceAll("\\s+", " ");
    return normalized;
  }
}
