/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static nl.b3p.planmonitorwonen.api.model.auth.PlanmonitorAuthentication.getFromSecurityContext;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Detailplanning;
import nl.b3p.planmonitorwonen.api.model.Plancategorie;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import nl.b3p.planmonitorwonen.api.model.PlanregistratieComplete;
import nl.b3p.planmonitorwonen.api.model.auth.PlanmonitorAuthentication;
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

  private final PlanmonitorWonenDatabaseService pmwDb;

  public PlanregistratieController(
      PlanmonitorWonenDatabaseService planmonitorWonenDatabaseService) {
    this.pmwDb = planmonitorWonenDatabaseService;
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/planregistraties")
  public Set<Planregistratie> planregistraties(@RequestParam(required = false) boolean details) {
    PlanmonitorAuthentication auth = getFromSecurityContext();
    Set<Planregistratie> planregistraties;
    if (auth.isProvincie()) {
      planregistraties = pmwDb.getPlanregistratiesForProvincie();
    } else {
      planregistraties = pmwDb.getPlanregistratiesForGemeentes(auth.getGemeentes());
    }
    if (details) {
      Map<String, Planregistratie> planregistratieMap =
          planregistraties.stream()
              .collect(Collectors.toMap(Planregistratie::getId, Function.identity()));
      Map<String, Planregistratie> plancategorieToPlanregistratieMap = new HashMap<>();
      Set<Plancategorie> plancategorieen =
          auth.isProvincie()
              ? pmwDb.getAllPlancategorieen()
              : pmwDb.getAllPlancategorieenForGemeentes(auth.getGemeentes());
      plancategorieen.forEach(
          plancategorie -> {
            Planregistratie planregistratie =
                planregistratieMap.get(plancategorie.planregistratieId());
            planregistratie.getPlancategorieList().add(plancategorie);
            plancategorieToPlanregistratieMap.put(plancategorie.id(), planregistratie);
          });
      Set<Detailplanning> detailplanningen =
          auth.isProvincie()
              ? pmwDb.getAllDetailplanningen()
              : pmwDb.getAllDetailplanningenForGemeentes(auth.getGemeentes());
      detailplanningen.forEach(
          detailplanning -> {
            Planregistratie planregistratie =
                plancategorieToPlanregistratieMap.get(detailplanning.plancategorieId());
            planregistratie.getDetailplanningList().add(detailplanning);
          });
    }
    return planregistraties;
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/planregistratie/{id}/details")
  public Map<String, Object> details(@PathVariable("id") String id) {
    PlanmonitorAuthentication auth = getFromSecurityContext();

    String gemeente = pmwDb.getPlanregistratieGemeente(id);
    if (gemeente == null) {
      throw new ResponseStatusException(NOT_FOUND);
    }

    if (!auth.isProvincie() && !auth.getGemeentes().contains(gemeente)) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to access plan id {} of gemeente {}, denied",
          auth.getTmApiAuthentication().getName(),
          auth.getGemeentes(),
          id,
          gemeente);
      throw new ResponseStatusException(NOT_FOUND);
    }

    return Map.of(
        "plancategorieen",
        pmwDb.getPlancategorieen(id),
        "detailplanningen",
        pmwDb.getDetailplanningen(id));
  }

  @PutMapping(path = "${planmonitor-wonen-api.base-path}/planregistratie/{id}")
  public ResponseEntity<?> put(
      @PathVariable("id") String id, @RequestBody PlanregistratieComplete planregistratieComplete)
      throws ParseException {

    if (id == null || !id.equals(planregistratieComplete.planregistratie().getId())) {
      throw new ResponseStatusException(BAD_REQUEST);
    }

    PlanmonitorAuthentication auth = getFromSecurityContext();

    if (auth.isProvincie()) {
      logger.warn(
          "Provincie user \"{}\" tried to save plan id {}, denied",
          auth.getTmApiAuthentication().getName(),
          id);
      throw new ResponseStatusException(FORBIDDEN);
    }

    if (!auth.getGemeentes().contains(planregistratieComplete.planregistratie().getGemeente())) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to save plan id {}, name \"{}\" with gemeente value {}, denied",
          auth.getTmApiAuthentication().getName(),
          auth.getGemeentes(),
          id,
          planregistratieComplete.planregistratie().getPlanNaam(),
          planregistratieComplete.planregistratie().getGemeente());
      throw new ResponseStatusException(FORBIDDEN);
    }

    String gemeente = pmwDb.getPlanregistratieGemeente(id);

    if (gemeente != null && !auth.isProvincie() && !auth.getGemeentes().contains(gemeente)) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to update plan id {}, name \"{}\" of gemeente {}, denied",
          auth.getTmApiAuthentication().getName(),
          auth.getGemeentes(),
          id,
          planregistratieComplete.planregistratie().getPlanNaam(),
          gemeente);
      throw new ResponseStatusException(FORBIDDEN);
    }

    pmwDb.insertPlanregistratie(
        planregistratieComplete.planregistratie(),
        planregistratieComplete.plancategorieen(),
        planregistratieComplete.detailplanningen());
    return ResponseEntity.status(OK).build();
  }

  @DeleteMapping(path = "${planmonitor-wonen-api.base-path}/planregistratie/{id}")
  public ResponseEntity<?> delete(@PathVariable("id") String id) {
    if (id == null) {
      throw new ResponseStatusException(BAD_REQUEST);
    }

    PlanmonitorAuthentication auth = getFromSecurityContext();

    if (auth.isProvincie()) {
      logger.warn(
          "Provincie user \"{}\" tried to delete plan id {}, denied",
          auth.getTmApiAuthentication().getName(),
          id);
      throw new ResponseStatusException(FORBIDDEN);
    }

    String gemeente = pmwDb.getPlanregistratieGemeente(id);

    if (gemeente == null) {
      throw new ResponseStatusException(NOT_FOUND);
    }

    if (!auth.getGemeentes().contains(gemeente)) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to delete plan id {} of gemeente {}, denied",
          auth.getTmApiAuthentication().getName(),
          auth.getGemeentes(),
          id,
          gemeente);
      throw new ResponseStatusException(FORBIDDEN);
    }

    pmwDb.deletePlanregistratie(id);
    return ResponseEntity.status(OK).build();
  }
}
