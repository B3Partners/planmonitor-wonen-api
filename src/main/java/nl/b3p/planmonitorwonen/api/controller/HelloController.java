/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
  private final PlanmonitorAuthenticationService planmonitorAuthenticationService;

  public HelloController(PlanmonitorAuthenticationService planmonitorAuthenticationService) {
    this.planmonitorAuthenticationService = planmonitorAuthenticationService;
  }

  @GetMapping(
      path = "${planmonitor-wonen-api.base-path}/hello",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> hello() {
    Map<String, Set<Object>> publicGroupProperties =
        planmonitorAuthenticationService.getGroupProperties().entrySet().stream()
            .map(
                entry ->
                    Map.entry(
                        entry.getKey(),
                        entry.getValue().stream()
                            .filter(
                                PlanmonitorAuthenticationService.AdminAdditionalProperty::isPublic)
                            .map(PlanmonitorAuthenticationService.AdminAdditionalProperty::value)
                            .collect(Collectors.toSet())))
            .filter(entry -> !entry.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        planmonitorAuthenticationService.getFromSecurityContext();

    return Map.of(
        "name",
        auth.userDetails().getUsername(),
        "authorities",
        auth.userDetails().getAuthorities().stream().map(Object::toString).toList(),
        "groupProperties",
        publicGroupProperties);
  }
}
