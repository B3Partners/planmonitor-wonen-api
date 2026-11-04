/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import nl.b3p.planmonitorwonen.api.AantallenRows.DetailplanningRow;
import nl.b3p.planmonitorwonen.api.AantallenRows.NewPlancategorie;
import nl.b3p.planmonitorwonen.api.AantallenRows.PlancategorieRow;
import nl.b3p.planmonitorwonen.api.model.Aantal;
import nl.b3p.planmonitorwonen.api.model.Aantallen;
import org.junit.jupiter.api.Test;

class AantallenRowsTest {
  private static final UUID PLAN = UUID.randomUUID();

  @Test
  void rowsToAantallen() {
    UUID nieuwbouw = UUID.randomUUID();
    UUID eengezins = UUID.randomUUID();
    List<PlancategorieRow> rows = List.of(
        new PlancategorieRow(nieuwbouw, PLAN, "Nieuwbouw", null, null, null, null, null, 10, 1),
        new PlancategorieRow(eengezins, PLAN, null, "Eengezins", null, null, null, null, 10, 1),
        new PlancategorieRow(UUID.randomUUID(), PLAN, null, null, null, null, null, "Sloop", 2, 0));
    Map<UUID, List<DetailplanningRow>> detailplanningen = Map.of(
        nieuwbouw,
        List.of(new DetailplanningRow(nieuwbouw, 2026, 5), new DetailplanningRow(nieuwbouw, 2025, 4)),
        eengezins,
        List.of(new DetailplanningRow(eengezins, 2025, 9)));

    Aantallen aantallen = AantallenRows.toAantallen(rows, detailplanningen);

    assertEquals(new Aantal(10, 1, new TreeMap<>(Map.of(2025, 4, 2026, 5))), aantallen.bruto());
    assertEquals(
        List.of(2025, 2026), List.copyOf(aantallen.bruto().perJaar().keySet()));
    assertEquals(new Aantal(2, 0, new TreeMap<>()), aantallen.sloop());
    assertEquals(Map.of("Eengezins", new Aantal(10, 1, new TreeMap<>(Map.of(2025, 9)))), aantallen.woningType());
    assertEquals(Map.of(), aantallen.betaalbaarheid());
  }

  @Test
  void aantallenToPlancategorieen() {
    Aantal aantal = new Aantal(10, 0, new TreeMap<>());
    Aantallen aantallen = new Aantallen(
        aantal, null, Map.of("Meergezins", aantal), null, Map.of(), Map.of("Sociale huur", aantal));

    assertEquals(
        List.of(
            new NewPlancategorie("nieuwbouw", "Nieuwbouw", aantal),
            new NewPlancategorie("woning_type", "Meergezins", aantal),
            new NewPlancategorie("betaalbaarheid", "Sociale huur", aantal)),
        AantallenRows.toPlancategorieen(aantallen));
    assertEquals(List.of(), AantallenRows.toPlancategorieen(null));
  }
}
