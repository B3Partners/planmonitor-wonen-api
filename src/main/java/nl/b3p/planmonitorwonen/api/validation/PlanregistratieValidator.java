/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

import java.util.ArrayList;
import java.util.List;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;

/**
 * Validates a planregistratie received from a client. Only the fields of the basisset 2.0 and the fields needed to
 * identify and authorize the planregistratie are required, the provincie specific fields are optional because
 * planregistraties may be delivered by software of a gemeente. Optional fields left out or null are not filled in, but
 * when filled in the value must be valid.
 *
 * <p>Whether the geometry lies within the gemeente is checked by
 * {@link nl.b3p.planmonitorwonen.api.service.PlanregistratieCompletionService} because it needs the database.
 */
public final class PlanregistratieValidator {
  public static final int PLAN_NAAM_MIN_LENGTH = 3;

  /**
   * The extent of the Netherlands in RD New (EPSG:28992) coordinates. Catches coordinates in another coordinate
   * system such as WGS84.
   */
  static final Envelope RD_EXTENT = new Envelope(-7000, 300000, 289000, 629000);

  private PlanregistratieValidator() {}

  /**
   * Returns the validation errors of the planregistratie, empty when valid.
   *
   * @param enumValues the values of the enum types in the database
   */
  public static List<ValidationError> validate(Planregistratie planregistratie, EnumValues enumValues) {
    List<ValidationError> errors = new ArrayList<>();
    validatePlanNaam(errors, planregistratie.getPlanNaam());
    required(errors, "gemeente", "Gemeente", planregistratie.getGemeente());
    requiredEnum(
        errors, "plantype", "Plantype", planregistratie.getPlantype(), enumValues.get(EnumValues.PLANTYPE));
    requiredEnum(
        errors,
        "statusPlanologisch",
        "Planologische planstatus",
        planregistratie.getStatusPlanologisch(),
        enumValues.get(EnumValues.STATUS_PLANOLOGISCH));
    optionalEnum(
        errors,
        "vertrouwelijkheid",
        planregistratie.getVertrouwelijkheid(),
        enumValues.get(EnumValues.VERTROUWELIJKHEID));
    optionalEnum(
        errors,
        "opdrachtgeverType",
        planregistratie.getOpdrachtgeverType(),
        enumValues.get(EnumValues.OPDRACHTGEVER_TYPE));
    optionalEnum(
        errors, "statusProject", planregistratie.getStatusProject(), enumValues.get(EnumValues.STATUS_PROJECT));
    optionalEnum(
        errors,
        "beoogdWoonmilieuAbf13",
        planregistratie.getBeoogdWoonmilieuAbf13(),
        enumValues.get(EnumValues.BEOOGD_WOONMILIEU_ABF13));
    String[] knelpunten = planregistratie.getKnelpuntenMeerkeuze();
    if (knelpunten != null) {
      for (int i = 0; i < knelpunten.length; i++) {
        optionalEnum(
            errors,
            "knelpuntenMeerkeuze[" + i + "]",
            knelpunten[i],
            enumValues.get(EnumValues.KNELPUNTEN_MEERKEUZE));
        if (knelpunten[i] == null) {
          errors.add(new ValidationError("knelpuntenMeerkeuze[" + i + "]", "Knelpunt mag niet leeg zijn"));
        }
      }
    }
    Integer aantalStudentenwoningen = planregistratie.getAantalStudentenwoningen();
    if (aantalStudentenwoningen != null && aantalStudentenwoningen < 0) {
      errors.add(
          new ValidationError("aantalStudentenwoningen", "Aantal studentenwoningen mag niet negatief zijn"));
    }
    validateGeometrie(errors, planregistratie.getGeometrie());
    AantallenValidator.validate(planregistratie.getAantallen(), enumValues, errors);
    return errors;
  }

  private static boolean required(List<ValidationError> errors, String field, String label, String value) {
    if (value == null || value.isBlank()) {
      errors.add(new ValidationError(field, label + " is verplicht"));
      return false;
    }
    return true;
  }

  private static void validatePlanNaam(List<ValidationError> errors, String planNaam) {
    if (required(errors, "planNaam", "Plannaam", planNaam)
        && planNaam.strip().length() < PLAN_NAAM_MIN_LENGTH) {
      errors.add(new ValidationError(
          "planNaam", "Plannaam moet minimaal %d tekens lang zijn".formatted(PLAN_NAAM_MIN_LENGTH)));
    }
  }

  private static void requiredEnum(
      List<ValidationError> errors, String field, String label, String value, List<String> values) {
    if (required(errors, field, label, value)) {
      optionalEnum(errors, field, value, values);
    }
  }

  private static void optionalEnum(List<ValidationError> errors, String field, String value, List<String> values) {
    if (value != null && !values.contains(value)) {
      errors.add(new ValidationError(
          field,
          "Onbekende waarde \"%s\", mogelijke waarden: %s".formatted(value, String.join(", ", values))));
    }
  }

  private static void validateGeometrie(List<ValidationError> errors, String wkt) {
    if (wkt == null || wkt.isBlank()) {
      errors.add(new ValidationError("geometrie", "Geometrie is verplicht"));
      return;
    }
    Geometry geometry;
    try {
      geometry = new WKTReader().read(wkt);
    } catch (ParseException | IllegalArgumentException e) {
      // IllegalArgumentException is thrown for a ring which is not closed or has too few points
      errors.add(new ValidationError("geometrie", "Geometrie is geen geldige WKT: " + e.getMessage()));
      return;
    }
    if (!(geometry instanceof Polygon)) {
      errors.add(new ValidationError(
          "geometrie", "Geometrie moet een polygoon zijn, geen " + geometry.getGeometryType()));
      return;
    }
    if (geometry.isEmpty()) {
      errors.add(new ValidationError("geometrie", "Geometrie is leeg"));
      return;
    }
    if (!RD_EXTENT.contains(geometry.getEnvelopeInternal())) {
      errors.add(new ValidationError(
          "geometrie", "Geometrie ligt niet in Nederland, de coördinaten moeten in RD (EPSG:28992) zijn"));
      return;
    }
    TopologyValidationError validationError = new IsValidOp(geometry).getValidationError();
    if (validationError != null) {
      errors.add(new ValidationError("geometrie", "Geometrie is ongeldig: " + validationError));
    }
  }
}
