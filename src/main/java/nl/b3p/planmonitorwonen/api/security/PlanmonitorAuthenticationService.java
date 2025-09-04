/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.security;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tailormap.api.security.TailormapUserDetails;

@Service
public class PlanmonitorAuthenticationService {
  public record PlanmonitorAuthentication(
      TailormapUserDetails userDetails, boolean isProvincie, Set<String> gemeentes) {}

  public PlanmonitorAuthentication getFromSecurityContext() throws ResponseStatusException {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new ResponseStatusException(UNAUTHORIZED);
    }
    if (!(authentication.getPrincipal() instanceof TailormapUserDetails userDetails)) {
      throw new ResponseStatusException(UNAUTHORIZED);
    }

    // these keys have been registered in the TM API, see:
    // https://github.com/Tailormap/tailormap-api/blob/d4be62bc4d1bf8ed8cdb52f0887590f1fed337f0/src/main/java/org/tailormap/api/persistence/helper/AdminAdditionalPropertyHelper.java#L18-L24
    boolean isProvincie =
        userDetails.getAdditionalGroupProperties().stream()
            .filter(p -> "typeGebruiker".equals(p.key()))
            .map(TailormapUserDetails.UDAdditionalProperty::value)
            .anyMatch("provincie"::equals);

    Set<String> gemeentes =
        userDetails.getAdditionalGroupProperties().stream()
            .filter(p -> "gemeente".equals(p.key()))
            .map(TailormapUserDetails.UDAdditionalProperty::value)
            .map(Object::toString)
            .collect(Collectors.toSet());

    PlanmonitorAuthentication result =
        new PlanmonitorAuthentication(userDetails, isProvincie, gemeentes);

    if (!result.isProvincie && result.gemeentes.isEmpty()) {
      throw new ResponseStatusException(FORBIDDEN);
    }

    return result;
  }
}
