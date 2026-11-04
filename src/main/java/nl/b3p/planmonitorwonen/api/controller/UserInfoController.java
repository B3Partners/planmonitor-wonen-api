/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import java.util.HashMap;
import java.util.Map;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.tailormap.api.security.TailormapAdditionalProperty;
import org.tailormap.api.security.TailormapUserDetails;

/**
 * Shows the logged-in user as seen by this API, to debug sharing the Tailormap login session. Same as the /user
 * endpoint of other Tailormap backends, plus the result of
 * {@link PlanmonitorAuthenticationService#requirePlanmonitorUser()}. Requires a login but no gemeente or provincie.
 */
@RestController
@Profile("!test")
public class UserInfoController {
  private final PlanmonitorAuthenticationService authenticationService;

  public UserInfoController(PlanmonitorAuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/user", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> user() {
    Authentication authentication = authenticationService.requireAuthenticated();

    Map<String, Object> response = new HashMap<>();
    response.put("username", authentication.getName());
    response.put(
        "authorities",
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList());

    if (authentication.getPrincipal() instanceof TailormapUserDetails userDetails) {
      response.put("organisation", userDetails.getOrganisation());
      response.put(
          "additionalProperties",
          userDetails.getAdditionalProperties().stream()
              .filter(TailormapAdditionalProperty::isPublic)
              .toList());
      response.put(
          "additionalGroupProperties",
          userDetails.getAdditionalGroupProperties().stream()
              .filter(TailormapAdditionalProperty::isPublic)
              .toList());
    }

    try {
      PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
          authenticationService.requirePlanmonitorUser();
      response.put("isProvincie", auth.isProvincie());
      response.put("gemeentes", auth.gemeentes());
    } catch (ResponseStatusException e) {
      response.put("planmonitorWonenError", e.getReason());
    }
    return response;
  }
}
