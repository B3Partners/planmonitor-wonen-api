/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

import java.util.ArrayList;
import java.util.List;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * Validates a planregistratie received from a client. Only the fields of the basisset 2.0 and the fields needed to
 * identify and authorize the planregistratie are required, the provincie specific fields are optional because
 * planregistraties may be delivered by software of a gemeente.
 */
public final class PlanregistratieValidator {
  private PlanregistratieValidator() {}

  public static List<ValidationError> validate(Planregistratie planregistratie) {
    List<ValidationError> errors = new ArrayList<>();
    required(errors, "planNaam", "Plannaam", planregistratie.getPlanNaam());
    required(errors, "gemeente", "Gemeente", planregistratie.getGemeente());
    required(errors, "plantype", "Plantype", planregistratie.getPlantype());
    required(errors, "statusPlanologisch", "Planologische planstatus", planregistratie.getStatusPlanologisch());
    validateGeometrie(errors, planregistratie.getGeometrie());
    return errors;
  }

  private static void required(List<ValidationError> errors, String field, String label, String value) {
    if (value == null || value.isBlank()) {
      errors.add(new ValidationError(field, label + " is verplicht"));
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
    } catch (ParseException e) {
      errors.add(new ValidationError("geometrie", "Geometrie is geen geldige WKT: " + e.getMessage()));
      return;
    }
    if (!(geometry instanceof Polygon)) {
      errors.add(new ValidationError(
          "geometrie", "Geometrie moet een polygoon zijn, geen " + geometry.getGeometryType()));
    }
  }
}
