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
    assertEquals(List.of(), PlanregistratieValidator.validate(minimal()));
  }

  @Test
  void reportsAllMissingRequiredFields() {
    List<ValidationError> errors = PlanregistratieValidator.validate(new Planregistratie().setPlanNaam(" "));
    assertEquals(
        List.of("planNaam", "gemeente", "plantype", "statusPlanologisch", "geometrie"),
        errors.stream().map(ValidationError::field).toList());
  }

  @Test
  void geometrieMustBeValidPolygon() {
    assertEquals(
        List.of(new ValidationError("geometrie", "Geometrie moet een polygoon zijn, geen Point")),
        PlanregistratieValidator.validate(minimal().setGeometrie("POINT (30000 390000)")));
    assertEquals(
        "geometrie",
        PlanregistratieValidator.validate(minimal().setGeometrie("POLYGON ((30000"))
            .getFirst()
            .field());
  }
}
