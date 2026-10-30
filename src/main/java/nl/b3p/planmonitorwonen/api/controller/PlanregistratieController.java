/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.UUID;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import nl.b3p.planmonitorwonen.api.service.PlanregistratieCompletionService;
import nl.b3p.planmonitorwonen.api.validation.PlanregistratieValidationException;
import nl.b3p.planmonitorwonen.api.validation.PlanregistratieValidator;
import nl.b3p.planmonitorwonen.api.validation.ValidationError;
import org.locationtech.jts.io.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("!test")
public class PlanregistratieController {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PlanmonitorAuthenticationService authenticationService;
  private final PlanmonitorWonenDatabaseService pmwDb;
  private final PlanregistratieCompletionService completionService;

  public PlanregistratieController(
      PlanmonitorAuthenticationService authenticationService,
      PlanmonitorWonenDatabaseService planmonitorWonenDatabaseService,
      PlanregistratieCompletionService completionService) {
    this.authenticationService = authenticationService;
    this.pmwDb = planmonitorWonenDatabaseService;
    this.completionService = completionService;
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/planregistraties")
  public List<Planregistratie> planregistraties(@RequestParam(required = false) boolean details) {
    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        authenticationService.getFromSecurityContext();
    if (auth.isProvincie()) {
      return pmwDb.getPlanregistratiesForProvincie(details);
    } else {
      return pmwDb.getPlanregistratiesForGemeentes(auth.gemeentes(), details);
    }
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/planregistratie/{id}")
  public Planregistratie planregistratie(@PathVariable("id") UUID id) {
    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        authenticationService.getFromSecurityContext();

    Planregistratie planregistratie =
        pmwDb.getPlanregistratie(id.toString()).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

    if (!auth.isProvincie() && !auth.gemeentes().contains(planregistratie.getGemeente())) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to access plan id {} of gemeente {}, denied",
          auth.authentication().getName(),
          auth.gemeentes(),
          id,
          planregistratie.getGemeente());
      throw new ResponseStatusException(NOT_FOUND);
    }
    return planregistratie;
  }

  @PutMapping(path = "${planmonitor-wonen-api.base-path}/planregistratie/{id}")
  public ResponseEntity<?> put(@PathVariable("id") UUID id, @RequestBody Planregistratie planregistratie)
      throws ParseException {

    if (planregistratie.getId() != null && !isSameUuid(id, planregistratie.getId())) {
      throw new ResponseStatusException(BAD_REQUEST, "Id in de body komt niet overeen met het id in het pad");
    }
    planregistratie.setId(id.toString());

    List<ValidationError> errors = PlanregistratieValidator.validate(planregistratie);
    if (!errors.isEmpty()) {
      throw new PlanregistratieValidationException(errors);
    }

    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        authenticationService.getFromSecurityContext();

    if (auth.isProvincie()) {
      logger.warn(
          "Provincie user \"{}\" tried to save plan id {}, denied",
          auth.authentication().getName(),
          id);
      throw new ResponseStatusException(FORBIDDEN);
    }

    if (!auth.gemeentes().contains(planregistratie.getGemeente())) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to save plan id {}, name \"{}\" with gemeente value {}, denied",
          auth.authentication().getName(),
          auth.gemeentes(),
          id,
          planregistratie.getPlanNaam(),
          planregistratie.getGemeente());
      throw new ResponseStatusException(FORBIDDEN);
    }

    String gemeente = pmwDb.getPlanregistratieGemeente(id.toString());

    if (gemeente != null && !auth.isProvincie() && !auth.gemeentes().contains(gemeente)) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to update plan id {}, name \"{}\" of gemeente {}, denied",
          auth.authentication().getName(),
          auth.gemeentes(),
          id,
          planregistratie.getPlanNaam(),
          gemeente);
      throw new ResponseStatusException(FORBIDDEN);
    }

    completionService.complete(planregistratie);
    pmwDb.insertPlanregistratie(planregistratie, auth.authentication().getName());
    return ResponseEntity.status(OK).build();
  }

  @DeleteMapping(path = "${planmonitor-wonen-api.base-path}/planregistratie/{id}")
  public ResponseEntity<?> delete(@PathVariable("id") UUID id) {
    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        authenticationService.getFromSecurityContext();

    if (auth.isProvincie()) {
      logger.warn(
          "Provincie user \"{}\" tried to delete plan id {}, denied",
          auth.authentication().getName(),
          id);
      throw new ResponseStatusException(FORBIDDEN);
    }

    String gemeente = pmwDb.getPlanregistratieGemeente(id.toString());

    if (gemeente == null) {
      throw new ResponseStatusException(NOT_FOUND);
    }

    if (!auth.gemeentes().contains(gemeente)) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to delete plan id {} of gemeente {}, denied",
          auth.authentication().getName(),
          auth.gemeentes(),
          id,
          gemeente);
      throw new ResponseStatusException(FORBIDDEN);
    }

    pmwDb.deletePlanregistratie(id.toString());
    return ResponseEntity.status(OK).build();
  }

  private static boolean isSameUuid(UUID id, String other) {
    try {
      return id.equals(UUID.fromString(other));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
