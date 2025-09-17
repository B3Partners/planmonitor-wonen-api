/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import java.util.Map;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
  private final PlanmonitorAuthenticationService planmonitorAuthenticationService;

  public HelloController(PlanmonitorAuthenticationService planmonitorAuthenticationService) {
    this.planmonitorAuthenticationService = planmonitorAuthenticationService;
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/hello", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> hello(@CurrentSecurityContext SecurityContext securityContext) {
    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        planmonitorAuthenticationService.getFromSecurityContext();

    return Map.of(
        "name",
        securityContext.getAuthentication().getName(),
        "authorities",
        securityContext.getAuthentication().getAuthorities().stream()
            .map(Object::toString)
            .toList(),
        "isProvincie",
        auth.isProvincie(),
        "gemeentes",
        auth.gemeentes());
  }
}
