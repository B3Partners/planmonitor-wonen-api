/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

import java.util.List;
import java.util.Map;

/**
 * The values of the enum types in the database, so the validators can report an unknown value with the possible values
 * instead of the database rejecting it. Read from the database at startup by
 * {@link nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService#getEnumValues()}, so the database schema is the
 * only place where the values are defined.
 *
 * @param values the values in enum sort order by enum type name
 */
public record EnumValues(Map<String, List<String>> values) {
  public static final String PLANTYPE = "pmw_plantype";
  public static final String STATUS_PLANOLOGISCH = "pmw_planologisch_status";
  public static final String VERTROUWELIJKHEID = "pmw_vertrouwelijkheid";
  public static final String OPDRACHTGEVER_TYPE = "pmw_opdrachtgever";
  public static final String STATUS_PROJECT = "pmw_project_status";
  public static final String KNELPUNTEN_MEERKEUZE = "pmw_knelpunten_meerkeuze";
  public static final String BEOOGD_WOONMILIEU_ABF13 = "pmw_woonmilieu_abf13";
  public static final String WONING_TYPE = "pmw_woning_type";
  public static final String WONEN_EN_ZORG = "pmw_wonen_en_zorg";
  public static final String FLEXWONINGEN = "pmw_flexwoningen";
  public static final String BETAALBAARHEID = "pmw_betaalbaarheid";

  public EnumValues {
    values = Map.copyOf(values);
  }

  /**
   * Returns the values of an enum type in enum sort order.
   *
   * @throws IllegalStateException when the enum type does not exist in the database
   */
  public List<String> get(String enumType) {
    List<String> result = values.get(enumType);
    if (result == null) {
      throw new IllegalStateException("Enum type %s not found in database".formatted(enumType));
    }
    return result;
  }
}
