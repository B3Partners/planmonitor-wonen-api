/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Number of dwellings for a plancategorie. A year left out of perJaar is not filled in, a year with 0 is filled in as
 * zero. For the totals both count as zero.
 *
 * @param gepland total number of planned dwellings, null is the same as 0
 * @param gerealiseerd number of dwellings already realised, null is the same as 0
 * @param perJaar planned number of dwellings per year of delivery (fasering), years with a null value are left out
 */
public record Aantal(Integer gepland, Integer gerealiseerd, SortedMap<Integer, Integer> perJaar) {
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
