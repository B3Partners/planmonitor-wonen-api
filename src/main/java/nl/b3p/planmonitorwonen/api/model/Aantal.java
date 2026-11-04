/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import nl.b3p.planmonitorwonen.api.validation.AantallenValidator;

/**
 * Number of dwellings for a plancategorie. A year left out of perJaar is not filled in, a year with 0 is filled in as
 * zero. For the totals both count as zero.
 *
 * @param gepland total number of planned dwellings, null is the same as 0
 * @param gerealiseerd number of dwellings already realised, null is the same as 0
 * @param perJaar planned number of dwellings per year of delivery (fasering), years with a null value are left out
 */
@Schema(
    description = "Aantal woningen van een categorie. In een response zijn `gepland` en `gerealiseerd` altijd"
        + " ingevuld en `perJaar` bevat alleen ingevulde jaren")
public record Aantal(
    @Schema(
        description = "Totaal aantal geplande woningen, weglaten of `null` telt als 0",
        minimum = "0",
        example = "20")
    Integer gepland,

    @Schema(
        description =
            "Aantal al gerealiseerde woningen, niet meer dan `gepland`. Weglaten of `null` telt als" + " 0",
        minimum = "0",
        example = "0")
    Integer gerealiseerd,

    @Schema(
        description = "Aantal geplande woningen per jaar van oplevering (fasering), van "
            + AantallenValidator.EERSTE_JAAR
            + " tot en met "
            + AantallenValidator.LAATSTE_JAAR
            + ". Een jaar weglaten of `null` betekent niet ingevuld, voor het totaal telt dat als 0. Behalve bij"
            + " sloop moet het totaal gelijk zijn aan `gepland` min `gerealiseerd`",
        example = "{\"2027\": 10, \"2028\": 10}")
    SortedMap<Integer, Integer> perJaar) {
  public Aantal {
    gepland = Objects.requireNonNullElse(gepland, 0);
    gerealiseerd = Objects.requireNonNullElse(gerealiseerd, 0);
    SortedMap<Integer, Integer> filled = new TreeMap<>();
    if (perJaar != null) {
      perJaar.entrySet().stream()
          .filter(e -> e.getKey() != null && e.getValue() != null)
          .forEach(e -> filled.put(e.getKey(), e.getValue()));
    }
    perJaar = filled;
  }
}
