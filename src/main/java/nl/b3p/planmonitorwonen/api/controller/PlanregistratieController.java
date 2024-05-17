/*
 * Copyright (C) 2024 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.util.Map;
import java.util.Set;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import nl.b3p.planmonitorwonen.api.model.PlanregistratieComplete;
import org.locationtech.jts.io.ParseException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("!test")
public class PlanregistratieController {
  private final PlanmonitorWonenDatabaseService pmwDb;

  public PlanregistratieController(
      PlanmonitorWonenDatabaseService planmonitorWonenDatabaseService) {
    this.pmwDb = planmonitorWonenDatabaseService;
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/planregistraties")
  public Set<Planregistratie> planregistraties() {
    return pmwDb.getPlanregistraties();
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/planregistratie/{id}/details")
  public Map<String, Object> details(@PathVariable("id") String id) {
    if (!pmwDb.planregistratieExists(id)) {
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
    pmwDb.insertPlanregistratie(
        planregistratieComplete.planregistratie(),
        planregistratieComplete.plancategorieen(),
        planregistratieComplete.detailplanningen());
    return ResponseEntity.status(OK).build();
  }

  @DeleteMapping(path = "${planmonitor-wonen-api.base-path}/planregistratie/{id}")
  public ResponseEntity<?> delete(@PathVariable("id") String id) {
    if (id == null) {
      return ResponseEntity.status(BAD_REQUEST).build();
    } else if (!pmwDb.planregistratieExists(id)) {
      return ResponseEntity.status(NOT_FOUND).build();
    } else {
      pmwDb.deletePlanregistratie(id);
      return ResponseEntity.status(OK).build();
    }
  }
}
