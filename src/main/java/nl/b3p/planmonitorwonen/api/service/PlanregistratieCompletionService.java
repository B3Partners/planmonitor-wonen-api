/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.service;

import java.lang.invoke.MethodHandles;
import java.util.List;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import nl.b3p.planmonitorwonen.api.validation.PlanregistratieValidationException;
import nl.b3p.planmonitorwonen.api.validation.ValidationError;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Fills in fields of a planregistratie the server can determine: the provincie of the gemeente, a default
 * vertrouwelijkheid and the regio and woonmilieu from the geometry when not set by the client.
 */
@Service
@Profile("!test")
public class PlanregistratieCompletionService {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PlanmonitorWonenDatabaseService pmwDb;
  private final PlanregistratieAutofillService autofillService;

  public PlanregistratieCompletionService(
      PlanmonitorWonenDatabaseService pmwDb, PlanregistratieAutofillService autofillService) {
    this.pmwDb = pmwDb;
    this.autofillService = autofillService;
  }

  /**
   * Fills in the fields the server can determine.
   *
   * @param planregistratie a planregistratie which passed
   *     {@link nl.b3p.planmonitorwonen.api.validation.PlanregistratieValidator}
   */
  public void complete(Planregistratie planregistratie) throws ParseException {
    String provincie = pmwDb.getProvincieForGemeente(planregistratie.getGemeente())
        .orElseThrow(() -> new PlanregistratieValidationException(List.of(
            new ValidationError("gemeente", "Onbekende gemeente: " + planregistratie.getGemeente()))));
    planregistratie.setProvincie(provincie);

    if (isBlank(planregistratie.getVertrouwelijkheid())) {
      planregistratie.setVertrouwelijkheid("Openbaar");
    }

    if (isBlank(planregistratie.getRegio()) || isBlank(planregistratie.getBeoogdWoonmilieuAbf13())) {
      Geometry geometry = new WKTReader().read(planregistratie.getGeometrie());
      if (isBlank(planregistratie.getRegio())) {
        planregistratie.setRegio(first(autofillService.getRegios(geometry), "regio", planregistratie));
      }
      if (isBlank(planregistratie.getBeoogdWoonmilieuAbf13())) {
        planregistratie.setBeoogdWoonmilieuAbf13(
            first(autofillService.getWoonmilieus(geometry), "woonmilieu", planregistratie));
      }
    }
  }

  private static String first(List<String> values, String what, Planregistratie planregistratie) {
    if (values.isEmpty()) {
      logger.warn(
          "No {} found for geometry of plan id {}, name \"{}\", not set (WFS features not loaded?)",
          what,
          planregistratie.getId(),
          planregistratie.getPlanNaam());
      return null;
    }
    return values.getFirst();
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
