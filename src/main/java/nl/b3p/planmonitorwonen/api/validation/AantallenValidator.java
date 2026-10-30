/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import nl.b3p.planmonitorwonen.api.model.Aantal;
import nl.b3p.planmonitorwonen.api.model.Aantallen;

/**
 * Validates the aantallen of a planregistratie with the same rules as the categorie table in the frontend. A categorie
 * or year not filled in counts as zero.
 *
 * <ul>
 *   <li>the numbers are not negative and gerealiseerd is not more than gepland
 *   <li>the years are within the years shown in the frontend
 *   <li>for each categorie except sloop the numbers per year add up to gepland minus gerealiseerd
 *   <li>for each categorie group the gepland numbers add up to bruto
 *   <li>for each categorie group and year the numbers add up to the number of bruto for that year
 * </ul>
 */
public final class AantallenValidator {
  /** The years shown in the frontend, for the first 10 years per year and then two periods of 5 years. */
  public static final int EERSTE_JAAR = 2024;

  public static final int LAATSTE_JAAR = 2043;

  /** The categorie values per group, the same as the enum types in the database. */
  static final Map<String, List<String>> GROEPEN = Map.of(
      "woningType", List.of("Eengezins", "Meergezins", "Onbekend"),
      "wonenEnZorg", List.of("Geclusterd", "Nultreden", "Onbekend", "Regulier", "Zorggeschikt"),
      "flexwoningen", List.of("Flexwoningen", "Regulier permanent"),
      "betaalbaarheid",
          List.of(
              "Sociale huur",
              "Huur middenhuur",
              "Huur dure huur",
              "Huur onbekend",
              "Koop betaalbare koop",
              "Koop dure koop",
              "Koop onbekend",
              "Onbekend koop of huur"));

  private static final String BRUTO_FIELD = "aantallen.bruto";

  private AantallenValidator() {}

  public static void validate(Aantallen aantallen, List<ValidationError> errors) {
    if (aantallen == null) {
      return;
    }
    Aantal bruto = aantallen.bruto();
    boolean valuesValid = validateAantal(BRUTO_FIELD, bruto, true, errors);
    valuesValid &= validateAantal("aantallen.sloop", aantallen.sloop(), false, errors);

    Map<String, Map<String, Aantal>> groepen = new LinkedHashMap<>();
    groepen.put("woningType", aantallen.woningType());
    groepen.put("wonenEnZorg", aantallen.wonenEnZorg());
    groepen.put("flexwoningen", aantallen.flexwoningen());
    groepen.put("betaalbaarheid", aantallen.betaalbaarheid());

    for (Map.Entry<String, Map<String, Aantal>> groep : groepen.entrySet()) {
      Map<String, Aantal> categorieen = Objects.requireNonNullElse(groep.getValue(), Map.of());
      for (Map.Entry<String, Aantal> categorie : categorieen.entrySet()) {
        String field = "aantallen." + groep.getKey() + "." + categorie.getKey();
        if (!GROEPEN.get(groep.getKey()).contains(categorie.getKey())) {
          errors.add(new ValidationError(
              field,
              "Onbekende categorie \"%s\", mogelijke waarden: %s"
                  .formatted(categorie.getKey(), String.join(", ", GROEPEN.get(groep.getKey())))));
          valuesValid = false;
        }
        valuesValid &= validateAantal(field, categorie.getValue(), true, errors);
      }
    }
    if (!valuesValid) {
      // The totals are only compared when the values themselves are valid
      return;
    }

    int brutoGepland = bruto == null ? 0 : bruto.gepland();
    for (Map.Entry<String, Map<String, Aantal>> groep : groepen.entrySet()) {
      Map<String, Aantal> categorieen = Objects.requireNonNullElse(groep.getValue(), Map.of());
      String field = "aantallen." + groep.getKey();
      int totaal = categorieen.values().stream()
          .filter(Objects::nonNull)
          .mapToInt(Aantal::gepland)
          .sum();
      if (totaal != brutoGepland) {
        errors.add(new ValidationError(
            field,
            "Totaal gepland van %s (%d) is niet gelijk aan bruto (%d)"
                .formatted(groep.getKey(), totaal, brutoGepland)));
      }

      SortedSet<Integer> jaren = new TreeSet<>();
      if (bruto != null) {
        jaren.addAll(bruto.perJaar().keySet());
      }
      categorieen.values().stream()
          .filter(Objects::nonNull)
          .forEach(a -> jaren.addAll(a.perJaar().keySet()));
      for (Integer jaar : jaren) {
        int brutoJaar = bruto == null ? 0 : bruto.perJaar().getOrDefault(jaar, 0);
        int jaarTotaal = categorieen.values().stream()
            .filter(Objects::nonNull)
            .mapToInt(a -> a.perJaar().getOrDefault(jaar, 0))
            .sum();
        if (jaarTotaal != brutoJaar) {
          errors.add(new ValidationError(
              field + ".perJaar." + jaar,
              "Totaal van %s in %d (%d) is niet gelijk aan bruto in %d (%d)"
                  .formatted(groep.getKey(), jaar, jaarTotaal, jaar, brutoJaar)));
        }
      }
    }
  }

  /**
   * Validates the values of an aantal and whether the numbers per year add up.
   *
   * @param restcapaciteitCheck whether the numbers per year must add up to gepland minus gerealiseerd
   * @return whether the values are valid, so the totals can be compared, regardless of the numbers per year adding up
   */
  private static boolean validateAantal(
      String field, Aantal aantal, boolean restcapaciteitCheck, List<ValidationError> errors) {
    if (aantal == null) {
      return true;
    }
    int errorCount = errors.size();
    if (aantal.gepland() < 0) {
      errors.add(new ValidationError(field + ".gepland", "Gepland mag niet negatief zijn"));
    }
    if (aantal.gerealiseerd() < 0) {
      errors.add(new ValidationError(field + ".gerealiseerd", "Gerealiseerd mag niet negatief zijn"));
    }
    if (aantal.gepland() >= 0 && aantal.gerealiseerd() > aantal.gepland()) {
      errors.add(new ValidationError(
          field + ".gerealiseerd",
          "Gerealiseerd (%d) mag niet meer zijn dan gepland (%d)"
              .formatted(aantal.gerealiseerd(), aantal.gepland())));
    }
    aantal.perJaar().forEach((jaar, aantalGepland) -> {
      if (jaar < EERSTE_JAAR || jaar > LAATSTE_JAAR) {
        errors.add(new ValidationError(
            field + ".perJaar." + jaar,
            "Jaar %d moet tussen %d en %d liggen".formatted(jaar, EERSTE_JAAR, LAATSTE_JAAR)));
      }
      if (aantalGepland < 0) {
        errors.add(new ValidationError(field + ".perJaar." + jaar, "Aantal mag niet negatief zijn"));
      }
    });
    if (errors.size() > errorCount) {
      return false;
    }
    if (restcapaciteitCheck) {
      int restcapaciteit = aantal.gepland() - aantal.gerealiseerd();
      int perJaarTotaal = aantal.perJaar().values().stream()
          .mapToInt(Integer::intValue)
          .sum();
      if (perJaarTotaal != restcapaciteit) {
        errors.add(new ValidationError(
            field + ".perJaar",
            "Totaal per jaar (%d) is niet gelijk aan gepland min gerealiseerd (%d)"
                .formatted(perJaarTotaal, restcapaciteit)));
      }
    }
    return true;
  }
}
