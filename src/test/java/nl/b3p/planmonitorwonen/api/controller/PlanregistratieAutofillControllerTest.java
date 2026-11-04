/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import nl.b3p.planmonitorwonen.api.service.PlanregistratieAutofillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
    controllers = PlanregistratieAutofillController.class,
    properties = {"planmonitor-wonen-api.base-path=/api/planmonitor-wonen", "spring.mvc.problemdetails.enabled=true"
    })
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration-test")
class PlanregistratieAutofillControllerTest {
  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private PlanmonitorAuthenticationService authenticationService;

  @MockitoBean
  private JdbcClient jdbcClient;

  @MockitoBean
  private PlanregistratieAutofillService autofillService;

  @Test
  void notLoggedInIsUnauthorized() {
    when(authenticationService.requirePlanmonitorUser())
        .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet ingelogd"));

    assertThat(mvc.post()
            .uri("/api/planmonitor-wonen/planregistratie/autofill-by-geometry")
            .contentType(MediaType.TEXT_PLAIN)
            .content("POLYGON ((30000 390000, 31000 390000, 31000 391000, 30000 391000, 30000 390000))"))
        .hasStatus(HttpStatus.UNAUTHORIZED);
    verifyNoInteractions(jdbcClient);
    // The autofill service is also called by its startup event listener, so only check the lookups
    verify(autofillService, never()).getProvincies(any());
    verify(autofillService, never()).getRegios(any());
    verify(autofillService, never()).getWoonmilieus(any());
  }
}
