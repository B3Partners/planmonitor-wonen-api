/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
    controllers = GemeentesController.class,
    properties = {"planmonitor-wonen-api.base-path=/api/planmonitor-wonen", "spring.mvc.problemdetails.enabled=true"
    })
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration-test")
class GemeentesControllerTest {
  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private PlanmonitorAuthenticationService authenticationService;

  @MockitoBean
  private JdbcClient jdbcClient;

  @Test
  void notLoggedInIsUnauthorized() {
    when(authenticationService.requireAuthenticated())
        .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet ingelogd"));

    assertThat(mvc.get().uri("/api/planmonitor-wonen/gemeentes")).hasStatus(HttpStatus.UNAUTHORIZED);
    verifyNoInteractions(jdbcClient);
  }
}
