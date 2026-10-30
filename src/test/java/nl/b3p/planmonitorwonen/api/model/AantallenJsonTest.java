/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AantallenJsonTest {
  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void nullMeansNotFilledInAndZeroIsKept() {
    Aantallen aantallen = mapper.readValue("""
{"bruto": {"gepland": 10, "perJaar": {"2025": 0, "2026": null, "2027": 10}},
"sloop": null,
"woningType": {"Eengezins": {"gepland": null, "gerealiseerd": null}}}""", Aantallen.class);

    assertEquals(new Aantal(10, 0, new TreeMap<>(Map.of(2025, 0, 2027, 10))), aantallen.bruto());
    assertEquals(null, aantallen.sloop());
    assertEquals(Map.of("Eengezins", new Aantal(0, 0, null)), aantallen.woningType());
  }

  @Test
  void categoriesNotFilledInAreLeftOut() {
    Aantallen aantallen = new Aantallen(
        new Aantal(10, 0, new TreeMap<>(Map.of(2027, 10))),
        null,
        Map.of("Meergezins", new Aantal(0, 0, null)),
        Map.of(),
        null,
        Map.of());

    assertEquals(
        "{\"bruto\":{\"gepland\":10,\"gerealiseerd\":0,\"perJaar\":{\"2027\":10}},"
            + "\"woningType\":{\"Meergezins\":{\"gepland\":0,\"gerealiseerd\":0,\"perJaar\":{}}}}",
        mapper.writeValueAsString(aantallen));
  }
}
