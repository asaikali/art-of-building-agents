package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MenuServiceTest {

  static MenuService service;

  @BeforeAll
  static void setUp() {
    service = new MenuService(new ObjectMapper());
  }

  @Test
  void loadsAllMenus() {
    var all = service.findAll();
    assertThat(all).isNotEmpty();
    assertThat(all).hasSizeGreaterThanOrEqualTo(24);
  }

  @Test
  void everyMenuHasAtLeastOneSection() {
    for (var menu : service.findAll()) {
      assertThat(menu.menuSections()).as("sections for %s", menu.restaurantId()).isNotEmpty();
    }
  }

  @Test
  void everySectionHasAtLeastOneItem() {
    for (var menu : service.findAll()) {
      for (var section : menu.menuSections()) {
        assertThat(section.items())
            .as("items in %s / %s", menu.restaurantId(), section.name())
            .isNotEmpty();
      }
    }
  }

  @Test
  void findByIdReturnsKnownMenu() {
    var result = service.findById("hys-steakhouse");
    assertThat(result).isPresent();
    assertThat(result.get().menuSections()).isNotEmpty();
  }

  @Test
  void findByIdReturnsEmptyForUnknown() {
    assertThat(service.findById("does-not-exist")).isEmpty();
  }

  @Test
  void menuItemsHaveRequiredFields() {
    for (var menu : service.findAll()) {
      for (var section : menu.menuSections()) {
        for (var item : section.items()) {
          assertThat(item.name())
              .as("item name in %s / %s", menu.restaurantId(), section.name())
              .isNotBlank();
          assertThat(item.currency())
              .as("currency for %s in %s", item.name(), menu.restaurantId())
              .isNotBlank();
        }
      }
    }
  }
}
