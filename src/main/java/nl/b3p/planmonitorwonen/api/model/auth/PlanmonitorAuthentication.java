/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model.auth;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import nl.b3p.planmonitorwonen.api.security.TMAPIAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public class PlanmonitorAuthentication {
  private TMAPIAuthenticationToken tmApiAuthentication;
  private boolean isProvincie;
  private Set<String> gemeentes = new HashSet<>();

  public TMAPIAuthenticationToken getTmApiAuthentication() {
    return tmApiAuthentication;
  }

  public boolean isProvincie() {
    return isProvincie;
  }

  public Set<String> getGemeentes() {
    return gemeentes;
  }

  public static PlanmonitorAuthentication getFromSecurityContext() throws ResponseStatusException {
    final TMAPIAuthenticationToken authentication =
        (TMAPIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new ResponseStatusException(UNAUTHORIZED);
    }

    Map<String, Set<String>> groupProperties = new HashMap<>();
    authentication
        .getAuthResponse()
        .get("groupProperties")
        .elements()
        .forEachRemaining(
            n -> {
              String key = n.get("key").textValue();
              String value = n.get("value").textValue();
              if (!groupProperties.containsKey(key)) {
                groupProperties.put(key, new HashSet<>());
              }
              groupProperties.get(key).add(value);
            });

    PlanmonitorAuthentication result = new PlanmonitorAuthentication();
    result.tmApiAuthentication = authentication;
    // these keys have been registered in the TM API, see:
    // -
    // https://github.com/Tailormap/tailormap-api/blob/d4be62bc4d1bf8ed8cdb52f0887590f1fed337f0/src/main/java/org/tailormap/api/persistence/helper/AdminAdditionalPropertyHelper.java#L18-L24
    result.isProvincie =
        groupProperties.getOrDefault("typeGebruiker", Collections.emptySet()).contains("provincie");
    result.gemeentes = groupProperties.getOrDefault("gemeente", Collections.emptySet());

    if (!result.isProvincie && result.gemeentes.isEmpty()) {
      throw new ResponseStatusException(FORBIDDEN);
    }

    return result;
  }
}
