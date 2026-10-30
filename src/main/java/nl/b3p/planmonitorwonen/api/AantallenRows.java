/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import nl.b3p.planmonitorwonen.api.model.Aantal;
import nl.b3p.planmonitorwonen.api.model.Aantallen;

/**
 * Converts between {@link Aantallen} and the plancategorie and detailplanning rows in the database. A plancategorie row
 * has exactly one of the categorie group columns set, the detailplanning rows hold the numbers per year.
 */
final class AantallenRows {
  private AantallenRows() {}

  record PlancategorieRow(
      UUID id,
      UUID planregistratieId,
      String nieuwbouw,
      String woningType,
      String wonenEnZorg,
      String flexwoningen,
      String betaalbaarheid,
      String sloop,
      Integer totaalGepland,
      Integer totaalGerealiseerd) {}

  record DetailplanningRow(UUID plancategorieId, Integer jaartal, Integer aantalGepland) {}

  /**
   * A plancategorie to insert.
   *
   * @param column the plancategorie column for the categorie group, a constant safe to use in SQL
   * @param value the categorie value
   */
  record NewPlancategorie(String column, String value, Aantal aantal) {}

  /**
   * @param rows the plancategorie rows of a single planregistratie
   * @param detailplanningen the detailplanning rows by plancategorie id
   */
  static Aantallen toAantallen(List<PlancategorieRow> rows, Map<UUID, List<DetailplanningRow>> detailplanningen) {
    Aantal bruto = null;
    Aantal sloop = null;
    Map<String, Aantal> woningType = new LinkedHashMap<>();
    Map<String, Aantal> wonenEnZorg = new LinkedHashMap<>();
    Map<String, Aantal> flexwoningen = new LinkedHashMap<>();
    Map<String, Aantal> betaalbaarheid = new LinkedHashMap<>();
    for (PlancategorieRow row : rows) {
      SortedMap<Integer, Integer> perJaar = new TreeMap<>();
      detailplanningen
          .getOrDefault(row.id(), List.of())
          .forEach(d -> perJaar.put(d.jaartal(), d.aantalGepland()));
      Aantal aantal = new Aantal(row.totaalGepland(), row.totaalGerealiseerd(), perJaar);
      if (row.nieuwbouw() != null) {
        bruto = aantal;
      } else if (row.sloop() != null) {
        sloop = aantal;
      } else if (row.woningType() != null) {
        woningType.put(row.woningType(), aantal);
      } else if (row.wonenEnZorg() != null) {
        wonenEnZorg.put(row.wonenEnZorg(), aantal);
      } else if (row.flexwoningen() != null) {
        flexwoningen.put(row.flexwoningen(), aantal);
      } else if (row.betaalbaarheid() != null) {
        betaalbaarheid.put(row.betaalbaarheid(), aantal);
      }
    }
    return new Aantallen(bruto, sloop, woningType, wonenEnZorg, flexwoningen, betaalbaarheid);
  }

  static List<NewPlancategorie> toPlancategorieen(Aantallen aantallen) {
    List<NewPlancategorie> result = new ArrayList<>();
    if (aantallen == null) {
      return result;
    }
    if (aantallen.bruto() != null) {
      result.add(new NewPlancategorie("nieuwbouw", "Nieuwbouw", aantallen.bruto()));
    }
    if (aantallen.sloop() != null) {
      result.add(new NewPlancategorie("sloop", "Sloop", aantallen.sloop()));
    }
    addGroep(result, "woning_type", aantallen.woningType());
    addGroep(result, "wonen_en_zorg", aantallen.wonenEnZorg());
    addGroep(result, "flexwoningen", aantallen.flexwoningen());
    addGroep(result, "betaalbaarheid", aantallen.betaalbaarheid());
    return result;
  }

  private static void addGroep(List<NewPlancategorie> result, String column, Map<String, Aantal> groep) {
    if (groep != null) {
      groep.forEach((value, aantal) -> {
        if (aantal != null) {
          result.add(new NewPlancategorie(column, value, aantal));
        }
      });
    }
  }
}
