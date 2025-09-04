/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import java.util.Map;
import java.util.stream.Collectors;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tailormap.api.security.TailormapUserDetails;

@RestController
public class HelloController {
  private final PlanmonitorAuthenticationService planmonitorAuthenticationService;

  public HelloController(PlanmonitorAuthenticationService planmonitorAuthenticationService) {
    this.planmonitorAuthenticationService = planmonitorAuthenticationService;
  }

  @GetMapping(
      path = "${planmonitor-wonen-api.base-path}/hello",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> hello(@AuthenticationPrincipal TailormapUserDetails userDetails) {
    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        planmonitorAuthenticationService.getFromSecurityContext();

    return Map.of(
        "name",
        userDetails.getUsername(),
        "authorities",
        auth.userDetails().getAuthorities().stream().map(Object::toString).toList(),
        "isProvincie",
        auth.isProvincie(),
        "gemeentes",
        auth.gemeentes(),
        "properties",
        userDetails.getAdditionalProperties().stream()
            .filter(TailormapUserDetails.UDAdditionalProperty::isPublic)
            .collect(
                Collectors.toMap(
                    TailormapUserDetails.UDAdditionalProperty::key,
                    TailormapUserDetails.UDAdditionalProperty::value)),
        "groupProperties",
        userDetails.getAdditionalGroupProperties().stream()
            .filter(TailormapUserDetails.UDAdditionalProperty::isPublic)
            .collect(
                Collectors.toMap(
                    TailormapUserDetails.UDAdditionalProperty::key,
                    TailormapUserDetails.UDAdditionalProperty::value)));
  }
}
