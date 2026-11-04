/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import nl.b3p.planmonitorwonen.api.controller.UserInfoController;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
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
class ApiVersionConfigurationTest {
  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private PlanmonitorAuthenticationService authenticationService;

  @BeforeEach
  void loggedIn() {
    when(authenticationService.requireAuthenticated()).thenReturn(new TestingAuthenticationToken("user", null));
    when(authenticationService.requirePlanmonitorUser())
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
  }

  @Test
  void currentVersionHasVersionHeader() {
    assertThat(mvc.get().uri("/api/planmonitor-wonen/v2/user"))
        .hasStatusOk()
        .hasHeader(ApiVersionConfiguration.VERSION_HEADER, "2.0");
  }

  @Test
  void unsupportedVersionIsBadRequest() {
    assertThat(mvc.get().uri("/api/planmonitor-wonen/v1/user")).hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(mvc.get().uri("/api/planmonitor-wonen/v3/user")).hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  void unversionedPathIsNotFound() {
    assertThat(mvc.get().uri("/api/planmonitor-wonen/user")).hasStatus(HttpStatus.NOT_FOUND);
  }
}
