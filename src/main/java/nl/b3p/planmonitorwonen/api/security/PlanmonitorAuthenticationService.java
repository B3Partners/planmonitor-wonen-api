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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tailormap.api.security.TailormapUserDetails;

@Service
@Profile("!test")
public class PlanmonitorAuthenticationService {
  public record PlanmonitorAuthentication(
      Authentication authentication, boolean isProvincie, Set<String> gemeentes) {}

  /**
   * Requires a logged-in Tailormap user, without requiring authorization for a gemeente or provincie (for data a
   * Tailormap admin also needs, such as the gemeente choices for the group properties).
   */
  public Authentication requireAuthenticated() throws ResponseStatusException {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // Without a session the authentication is an anonymous authentication instead of null
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      throw new ResponseStatusException(UNAUTHORIZED, "Niet ingelogd");
    }
    return authentication;
  }

  /**
   * Requires a logged-in Tailormap user with a group property typeGebruiker "provincie" or one or more gemeente group
   * properties.
   *
   * @return the provincie flag and the gemeentes the user is authorized for
   * @throws ResponseStatusException 401 when not logged in, 403 when not authorized for a gemeente or provincie
   */
  public PlanmonitorAuthentication requirePlanmonitorUser() throws ResponseStatusException {
    final Authentication authentication = requireAuthenticated();

    if (authentication.getPrincipal() instanceof TailormapUserDetails userDetails) {
      // these keys have been registered in the TM API, see:
      // https://github.com/Tailormap/tailormap-api/blob/main/src/main/java/org/tailormap/api/persistence/helper/AdminAdditionalPropertyHelper.java#L18-L24

      boolean isProvincie =
          userDetails.streamAllPropertiesForKey("typeGebruiker").anyMatch("provincie"::equals);

      Set<String> gemeentes = userDetails
          .streamAllPropertiesForKey("gemeente")
          .map(Object::toString)
          .collect(Collectors.toSet());
      PlanmonitorAuthentication result = new PlanmonitorAuthentication(authentication, isProvincie, gemeentes);

      if (!result.isProvincie && result.gemeentes.isEmpty()) {
        throw new ResponseStatusException(
            FORBIDDEN, "Gebruiker is niet geautoriseerd voor een gemeente of provincie");
      }
      return result;
    } else {
      throw new ResponseStatusException(FORBIDDEN, "Gebruiker is niet geautoriseerd voor Planmonitor Wonen");
    }
  }
}
