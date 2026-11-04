/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.junit.jupiter.api.Test;

class PlanregistratieValidatorTest {
  private static List<ValidationError> validate(Planregistratie planregistratie) {
    return PlanregistratieValidator.validate(planregistratie, TestEnumValues.ENUM_VALUES);
  }

  private static Planregistratie minimal() {
    return new Planregistratie()
        .setPlanNaam("Plan")
        .setGemeente("Middelburg")
        .setPlantype("Herstructurering")
        .setStatusPlanologisch("3. In voorbereiding")
        .setGeometrie("POLYGON ((30000 390000, 31000 390000, 31000 391000, 30000 391000, 30000 390000))");
  }

  @Test
  void provincieSpecificFieldsAreOptional() {
    assertEquals(List.of(), validate(minimal()));
  }

  @Test
  void reportsAllMissingRequiredFields() {
    List<ValidationError> errors = validate(new Planregistratie().setPlanNaam(" "));
    assertEquals(
        List.of("planNaam", "gemeente", "plantype", "statusPlanologisch", "geometrie"),
        errors.stream().map(ValidationError::field).toList());
  }

  @Test
  void geometrieMustBeValidPolygon() {
    assertEquals(
        List.of(new ValidationError("geometrie", "Geometrie moet een polygoon zijn, geen Point")),
        validate(minimal().setGeometrie("POINT (30000 390000)")));
    assertEquals(
        "geometrie",
        validate(minimal().setGeometrie("POLYGON ((30000")).getFirst().field());
  }

  @Test
  void enumFieldsMustHaveKnownValue() {
    List<ValidationError> errors = validate(minimal()
        .setPlantype("Onbekend")
        .setStatusPlanologisch("3")
        .setVertrouwelijkheid("Geheim")
        .setOpdrachtgeverType("Iemand")
        .setStatusProject("Klaar")
        .setBeoogdWoonmilieuAbf13("Stad")
        .setKnelpuntenMeerkeuze(new String[] {"Stikstof", "Netcongestie"}));
    assertEquals(
        List.of(
            "plantype",
            "statusPlanologisch",
            "vertrouwelijkheid",
            "opdrachtgeverType",
            "statusProject",
            "beoogdWoonmilieuAbf13",
            "knelpuntenMeerkeuze[1]"),
        errors.stream().map(ValidationError::field).toList());
    assertEquals(
        "Onbekende waarde \"Geheim\", mogelijke waarden: Gemeente, Openbaar, Provincie",
        errors.get(2).message());
  }

  @Test
  void optionalFieldsWithValidValues() {
    assertEquals(
        List.of(),
        validate(minimal()
            .setVertrouwelijkheid("Gemeente")
            .setOpdrachtgeverType("Woningbouwcorporatie")
            .setStatusProject("Realisatie tot eerste woning")
            .setBeoogdWoonmilieuAbf13("Geen Woonmilieu")
            .setKnelpuntenMeerkeuze(new String[] {"Grond/Didam", "Nuts", "Spuitzones/milieu"})
            .setAantalStudentenwoningen(0)));
  }

  @Test
  void knelpuntMustNotBeNull() {
    assertEquals(
        List.of(new ValidationError("knelpuntenMeerkeuze[0]", "Knelpunt mag niet leeg zijn")),
        validate(minimal().setKnelpuntenMeerkeuze(new String[] {null})));
  }

  @Test
  void planNaamMinimumLength() {
    assertEquals(
        List.of(new ValidationError("planNaam", "Plannaam moet minimaal 3 tekens lang zijn")),
        validate(minimal().setPlanNaam(" ab ")));
    assertEquals(List.of(), validate(minimal().setPlanNaam("abc")));
  }

  @Test
  void aantalStudentenwoningenNotNegative() {
    assertEquals(
        List.of(new ValidationError(
            "aantalStudentenwoningen", "Aantal studentenwoningen mag niet negatief zijn")),
        validate(minimal().setAantalStudentenwoningen(-1)));
  }

  @Test
  void geometrieMustBeInRd() {
    List<ValidationError> errors =
        validate(minimal().setGeometrie("POLYGON ((3.6 51.5, 3.7 51.5, 3.7 51.6, 3.6 51.6, 3.6 51.5))"));
    assertEquals(
        List.of(new ValidationError(
            "geometrie",
            "Geometrie ligt niet in Nederland, de coördinaten moeten in RD (EPSG:28992) zijn")),
        errors);
  }

  @Test
  void geometrieMustBeTopologicallyValid() {
    // Bow tie
    List<ValidationError> errors = validate(minimal()
        .setGeometrie("POLYGON ((30000 390000, 31000 391000, 31000 390000, 30000 391000, 30000 390000))"));
    assertEquals(
        List.of(new ValidationError(
            "geometrie",
            "Geometrie is ongeldig: Self-intersection at or near point (30500.0, 390500.0, NaN)")),
        errors);
  }

  @Test
  void geometrieRingNotClosed() {
    List<ValidationError> errors =
        validate(minimal().setGeometrie("POLYGON ((30000 390000, 31000 390000, 31000 391000, 30000 391000))"));
    assertEquals(
        List.of("geometrie"),
        errors.stream().map(ValidationError::field).toList());
  }

  @Test
  void geometrieNotEmpty() {
    assertEquals(
        List.of(new ValidationError("geometrie", "Geometrie is leeg")),
        validate(minimal().setGeometrie("POLYGON EMPTY")));
  }
}
