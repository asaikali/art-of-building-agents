package com.example.jarvis.requirements.alignment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TurnClassifierTest {

  private final TurnClassifier classifier = new TurnClassifier();

  @Test
  void recognizesAffirmativeReplies() {
    assertThat(classifier.isAffirmative("Yes!")).isTrue();
    assertThat(classifier.isAffirmative("sounds   good")).isTrue();
    assertThat(classifier.isAffirmative("no")).isFalse();
  }

  @Test
  void recognizesOpeners() {
    assertThat(classifier.isOpener("hello")).isTrue();
    assertThat(classifier.isOpener("Can you help?")).isTrue();
    assertThat(classifier.isOpener("plan a dinner")).isFalse();
  }

  @Test
  void recognizesUncertainReplies() {
    assertThat(classifier.isUncertain("not sure")).isTrue();
    assertThat(classifier.isUncertain("You decide.")).isTrue();
    assertThat(classifier.isUncertain("yes")).isFalse();
  }
}
