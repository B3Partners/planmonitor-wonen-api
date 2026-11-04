/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

import java.util.List;
import java.util.Map;

/**
 * The enum values of the database schema for unit tests without a database. PlanmonitorWonenDatabaseServiceTest checks
 * these are the same as the values read from the database after the migrations.
 */
public final class TestEnumValues {
  public static final EnumValues ENUM_VALUES = new EnumValues(Map.ofEntries(
      Map.entry(
          "pmw_betaalbaarheid",
          List.of(
              "Sociale huur",
              "Huur middenhuur",
              "Huur dure huur",
              "Huur onbekend",
              "Koop betaalbare koop",
              "Koop dure koop",
              "Koop onbekend",
              "Onbekend koop of huur")),
      Map.entry("pmw_flexwoningen", List.of("Flexwoningen", "Regulier permanent")),
      Map.entry(
          "pmw_knelpunten_meerkeuze",
          List.of(
              "Ambtelijke capaciteit",
              "Anders",
              "Bereikbaarheid",
              "Geluidshinder",
              "Grond/Didam",
              "Maatschappelijk draagvlak",
              "Onrendabele top",
              "Spuitzones/milieu",
              "Stikstof",
              "Vervuilde grond",
              "Flora/fauna",
              "Nuts",
              "Geen")),
      Map.entry("pmw_nieuwbouw", List.of("Nieuwbouw")),
      Map.entry(
          "pmw_opdrachtgever",
          List.of(
              "Anders",
              "Gemeente",
              "Meerdere namelijk",
              "Nog onbekend",
              "Particulieren",
              "Projectontwikkelaar",
              "Woningbouwcorporatie")),
      Map.entry(
          "pmw_planologisch_status",
          List.of(
              "1A. Onherroepelijk",
              "1B. Onherroepelijk, uitwerkingsplicht",
              "1C. Onherroepelijk, wijzigingsbevoegdheid",
              "2A. Vastgesteld",
              "2B. Vastgesteld, uitwerkingsplicht",
              "2C. Vastgesteld, wijzigingsbevoegdheid",
              "3. In voorbereiding",
              "4A. Visie",
              "4B. Optie",
              "5A. Ambtelijk met locatie",
              "5B. Ambtelijk zonder locatie")),
      Map.entry(
          "pmw_plantype",
          List.of(
              "Pand Transformatie",
              "Transformatiegebied",
              "Herstructurering",
              "Verdichting",
              "Uitbreiding uitleg",
              "Uitbreiding overig")),
      Map.entry(
          "pmw_project_status",
          List.of(
              "Studie",
              "Start",
              "Initiatief",
              "Definitie",
              "Ontwerp",
              "Voorbereiding",
              "Bouwvoorbereiding",
              "Realisatie tot eerste woning",
              "Realisatie tot laatste woning",
              "Nazorg",
              "Afgerond",
              "Onbekend")),
      Map.entry("pmw_sloop", List.of("Sloop")),
      Map.entry("pmw_vertrouwelijkheid", List.of("Gemeente", "Openbaar", "Provincie")),
      Map.entry("pmw_wonen_en_zorg", List.of("Geclusterd", "Nultreden", "Onbekend", "Regulier", "Zorggeschikt")),
      Map.entry("pmw_woning_type", List.of("Eengezins", "Meergezins", "Onbekend")),
      Map.entry(
          "pmw_woonmilieu_abf13",
          List.of(
              "Centrum",
              "Stedelijk Compact",
              "Stedelijk Grondgebonden",
              "Stedelijk met Groen",
              "Landelijk of Dorps bij de stad",
              "Dorps",
              "Landelijk",
              "Geen Woonmilieu"))));

  private TestEnumValues() {}
}
