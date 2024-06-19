/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model.auth;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.HashMap;
import java.util.Map;
import nl.b3p.planmonitorwonen.api.security.TMAPIAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public class PlanmonitorAuthentication {
  private TMAPIAuthenticationToken tmApiAuthentication;
  private boolean isProvincie;
  private String gemeente;

  public TMAPIAuthenticationToken getTmApiAuthentication() {
    return tmApiAuthentication;
  }

  public boolean isProvincie() {
    return isProvincie;
  }

  public String getGemeente() {
    return gemeente;
  }

  public static PlanmonitorAuthentication getFromSecurityContext() throws ResponseStatusException {
    final TMAPIAuthenticationToken authentication =
        (TMAPIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new ResponseStatusException(UNAUTHORIZED);
    }

    Map<String, String> groupProperties = new HashMap<>();
    authentication
        .getAuthResponse()
        .get("groupProperties")
        .elements()
        .forEachRemaining(
            n -> groupProperties.put(n.get("key").textValue(), n.get("value").textValue()));

    PlanmonitorAuthentication result = new PlanmonitorAuthentication();
    result.tmApiAuthentication = authentication;
    result.isProvincie = "provincie".equals(groupProperties.get("typeGebruiker"));
    result.gemeente = groupProperties.get("gemeente");

    if (!result.isProvincie && result.gemeente == null) {
      throw new ResponseStatusException(FORBIDDEN);
    }

    return result;
  }
}
