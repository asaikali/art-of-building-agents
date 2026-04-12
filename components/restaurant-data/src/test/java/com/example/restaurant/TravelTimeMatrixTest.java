package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TravelTimeMatrixTest {

  static TravelTimeMatrix matrix;

  @BeforeAll
  static void setUp() {
    matrix = new TravelTimeMatrix(new ObjectMapper());
  }

  @Test
  void loadsAllNeighborhoods() {
    assertThat(matrix.asMap()).isNotEmpty();
    assertThat(matrix.asMap()).hasSize(11);
    assertThat(matrix.asMap()).containsKeys("Financial District", "Downtown", "Danforth");
  }

  @Test
  void findsKnownBaseMinutes() {
    assertThat(matrix.findBaseMinutes("Financial District", "Financial District")).contains(10);
    assertThat(matrix.findBaseMinutes("Danforth", "King West")).contains(32);
    assertThat(matrix.findBaseMinutes("Harbourfront", "Entertainment District")).contains(12);
  }

  @Test
  void returnsEmptyForUnknownNeighborhood() {
    assertThat(matrix.findBaseMinutes("Does Not Exist", "Downtown")).isEmpty();
    assertThat(matrix.findBaseMinutes("Downtown", "Does Not Exist")).isEmpty();
  }

  @Test
  void returnsEmptyForNullInputs() {
    assertThat(matrix.findBaseMinutes(null, "Downtown")).isEmpty();
    assertThat(matrix.findBaseMinutes("Downtown", null)).isEmpty();
  }
}
