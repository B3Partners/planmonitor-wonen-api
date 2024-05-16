/*
 * Copyright (C) 2024 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import java.util.Set;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
public class PlanregistratieController {
  private PlanmonitorWonenDatabaseService planmonitorWonenDatabaseService;

  public PlanregistratieController(
      PlanmonitorWonenDatabaseService planmonitorWonenDatabaseService) {
    this.planmonitorWonenDatabaseService = planmonitorWonenDatabaseService;
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/planregistraties")
  public Set<Planregistratie> planregistraties() {
    return planmonitorWonenDatabaseService.getPlanregistraties();
  }
}
