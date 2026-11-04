/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService.PlanmonitorAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
    controllers = UserInfoController.class,
    properties = {"planmonitor-wonen-api.base-path=/api/planmonitor-wonen", "spring.mvc.problemdetails.enabled=true"
    })
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration-test")
class UserInfoControllerTest {
  private static final String URL = "/api/planmonitor-wonen/user";

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private PlanmonitorAuthenticationService authenticationService;

  @Test
  void notLoggedInIsUnauthorized() {
    when(authenticationService.requireAuthenticated())
        .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet ingelogd"));

    assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void showsUserWithoutGemeente() {
    when(authenticationService.requireAuthenticated())
        .thenReturn(new TestingAuthenticationToken("user", null, "admin"));
    when(authenticationService.requirePlanmonitorUser())
        .thenThrow(new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Gebruiker is niet geautoriseerd voor Planmonitor Wonen"));

    assertThat(mvc.get().uri(URL)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
{"username": "user", "authorities": ["admin"], "planmonitorWonenError": "Gebruiker is niet geautoriseerd voor Planmonitor Wonen"}""");
  }

  @Test
  void showsGemeentes() {
    TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", null);
    when(authenticationService.requireAuthenticated()).thenReturn(authentication);
    when(authenticationService.requirePlanmonitorUser())
        .thenReturn(new PlanmonitorAuthentication(authentication, false, Set.of("Middelburg")));

    assertThat(mvc.get().uri(URL)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
{"username": "user", "isProvincie": false, "gemeentes": ["Middelburg"]}""");
  }
}
