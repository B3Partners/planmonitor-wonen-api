/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import nl.b3p.planmonitorwonen.api.model.Aantal;
import nl.b3p.planmonitorwonen.api.model.Aantallen;
import org.junit.jupiter.api.Test;

class AantallenValidatorTest {
  private static Aantal aantal(int gepland, int gerealiseerd, Map<Integer, Integer> perJaar) {
    return new Aantal(gepland, gerealiseerd, new TreeMap<>(perJaar));
  }

  /** 20 dwellings, 5 realised, 10 in 2026 and 5 in 2027, divided over all groups. */
  private static Aantallen valid() {
    return new Aantallen(
        aantal(20, 5, Map.of(2026, 10, 2027, 5)),
        aantal(3, 0, Map.of()),
        Map.of(
            "Eengezins", aantal(12, 5, Map.of(2026, 7)),
            "Meergezins", aantal(8, 0, Map.of(2026, 3, 2027, 5)),
            "Onbekend", aantal(0, 0, Map.of(2026, 0))),
        Map.of("Regulier", aantal(20, 5, Map.of(2026, 10, 2027, 5))),
        Map.of("Regulier permanent", aantal(20, 5, Map.of(2026, 10, 2027, 5))),
        Map.of(
            "Sociale huur", aantal(10, 0, Map.of(2026, 5, 2027, 5)),
            "Koop dure koop", aantal(10, 5, Map.of(2026, 5))));
  }

  private static List<ValidationError> validate(Aantallen aantallen) {
    List<ValidationError> errors = new ArrayList<>();
    AantallenValidator.validate(aantallen, errors);
    return errors;
  }

  private static List<String> fields(List<ValidationError> errors) {
    return errors.stream().map(ValidationError::field).toList();
  }

  @Test
  void validAantallen() {
    assertEquals(List.of(), validate(valid()));
  }

  @Test
  void notFilledIn() {
    assertEquals(List.of(), validate(null));
    assertEquals(List.of(), validate(new Aantallen(null, null, null, null, null, null)));
  }

  @Test
  void sloopOnlyDoesNotNeedYears() {
    assertEquals(List.of(), validate(new Aantallen(null, aantal(5, 0, Map.of()), null, null, null, null)));
  }

  @Test
  void everyGroupMustAddUpToBruto() {
    Aantallen brutoOnly = new Aantallen(aantal(20, 0, Map.of(2027, 20)), null, null, null, null, null);
    assertEquals(
        List.of(
            "aantallen.woningType",
            "aantallen.woningType.perJaar.2027",
            "aantallen.wonenEnZorg",
            "aantallen.wonenEnZorg.perJaar.2027",
            "aantallen.flexwoningen",
            "aantallen.flexwoningen.perJaar.2027",
            "aantallen.betaalbaarheid",
            "aantallen.betaalbaarheid.perJaar.2027"),
        fields(validate(brutoOnly)));
    assertEquals(
        "Totaal gepland van woningType (0) is niet gelijk aan bruto (20)",
        validate(brutoOnly).getFirst().message());
  }

  @Test
  void groupWithoutBruto() {
    Aantallen aantallen =
        new Aantallen(null, null, Map.of("Eengezins", aantal(5, 0, Map.of(2026, 5))), null, null, null);
    assertEquals(List.of("aantallen.woningType", "aantallen.woningType.perJaar.2026"), fields(validate(aantallen)));
  }

  @Test
  void yearsMustAddUpToRestcapaciteit() {
    Aantallen v = valid();
    Aantallen aantallen = new Aantallen(
        v.bruto(),
        v.sloop(),
        Map.of(
            // 2027 moved to 2028, so the year totals of woningType do not add up either
            "Eengezins", aantal(12, 5, Map.of(2026, 7)),
            "Meergezins", aantal(8, 0, Map.of(2026, 3, 2028, 4))),
        v.wonenEnZorg(),
        v.flexwoningen(),
        v.betaalbaarheid());
    List<ValidationError> errors = validate(aantallen);
    assertEquals(
        List.of(
            "aantallen.woningType.Meergezins.perJaar",
            "aantallen.woningType.perJaar.2027",
            "aantallen.woningType.perJaar.2028"),
        fields(errors));
    assertEquals(
        "Totaal per jaar (7) is niet gelijk aan gepland min gerealiseerd (8)",
        errors.getFirst().message());
  }

  @Test
  void invalidValuesSkipTotals() {
    Aantallen v = valid();
    Aantallen aantallen = new Aantallen(
        v.bruto(),
        aantal(-1, 0, Map.of()),
        Map.of(
            "Tussenwoning", aantal(12, 5, Map.of(2026, 7)),
            "Meergezins", aantal(8, 9, Map.of(2050, 3, 2027, -5))),
        v.wonenEnZorg(),
        v.flexwoningen(),
        v.betaalbaarheid());
    List<ValidationError> errors = validate(aantallen);
    assertEquals(
        List.of(
            "aantallen.sloop.gepland",
            "aantallen.woningType.Meergezins.gerealiseerd",
            "aantallen.woningType.Meergezins.perJaar.2027",
            "aantallen.woningType.Meergezins.perJaar.2050",
            "aantallen.woningType.Tussenwoning"),
        fields(errors).stream().sorted().toList());
    assertEquals(
        "Onbekende categorie \"Tussenwoning\", mogelijke waarden: Eengezins, Meergezins, Onbekend",
        errors.stream()
            .filter(e -> e.field().equals("aantallen.woningType.Tussenwoning"))
            .findFirst()
            .orElseThrow()
            .message());
  }
}
