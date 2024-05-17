/*
 * Copyright (C) 2024 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Map;
import java.util.Set;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
