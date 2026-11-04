/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

class PlanmonitorAuthenticationServiceTest {
  private final PlanmonitorAuthenticationService service = new PlanmonitorAuthenticationService();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private HttpStatus statusOf(ResponseStatusException e) {
    return HttpStatus.valueOf(e.getStatusCode().value());
  }

  @Test
  void noAuthenticationIsUnauthorized() {
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        statusOf(assertThrows(ResponseStatusException.class, service::requirePlanmonitorUser)));
  }

  @Test
  void anonymousIsUnauthorized() {
    SecurityContextHolder.getContext()
        .setAuthentication(new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        statusOf(assertThrows(ResponseStatusException.class, service::requirePlanmonitorUser)));
  }

  @Test
  void otherUserIsForbidden() {
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", null));
    assertEquals(
        HttpStatus.FORBIDDEN,
        statusOf(assertThrows(ResponseStatusException.class, service::requirePlanmonitorUser)));
  }

  @Test
  void requireAuthenticatedRejectsAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        statusOf(assertThrows(ResponseStatusException.class, service::requireAuthenticated)));
  }

  @Test
  void requireAuthenticatedAllowsOtherUser() {
    TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    assertEquals(authentication, service.requireAuthenticated());
  }
}
