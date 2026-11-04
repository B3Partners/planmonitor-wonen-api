/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService.PlanmonitorAuthentication;
import nl.b3p.planmonitorwonen.api.service.PlanregistratieCompletionService;
import nl.b3p.planmonitorwonen.api.validation.EnumValues;
import nl.b3p.planmonitorwonen.api.validation.PlanNaamInUseException;
import nl.b3p.planmonitorwonen.api.validation.TestEnumValues;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
    controllers = PlanregistratieController.class,
    properties = {"planmonitor-wonen-api.base-path=/api/planmonitor-wonen", "spring.mvc.problemdetails.enabled=true"
    })
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration-test")
class PlanregistratieControllerTest {
  private static final String ID = "0b5c1d0e-3f43-4e4a-9a6e-2f1f6b6d2a10";
  private static final String URL = "/api/planmonitor-wonen/v2/planregistratie/" + ID;

  private static final String VALID_PLAN = """
{
"planNaam": "Plan",
"gemeente": "Middelburg",
"plantype": "Herstructurering",
"statusPlanologisch": "3. In voorbereiding",
"geometrie": "POLYGON ((30000 390000, 31000 390000, 31000 391000, 30000 391000, 30000 390000))"
}""";

  @TestConfiguration
  static class EnumValuesConfiguration {
    @Bean
    EnumValues enumValues() {
      return TestEnumValues.ENUM_VALUES;
    }
  }

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private PlanmonitorAuthenticationService authenticationService;

  @MockitoBean
  private PlanmonitorWonenDatabaseService pmwDb;

  @MockitoBean
  private PlanregistratieCompletionService completionService;

  private void loggedInAsGemeente() {
    when(authenticationService.requirePlanmonitorUser())
        .thenReturn(new PlanmonitorAuthentication(
            new TestingAuthenticationToken("gemeente", null), false, Set.of("Middelburg")));
  }

  private void savedPlanIsReturned() {
    when(pmwDb.getPlanregistratie(ID))
        .thenReturn(Optional.of(new Planregistratie()
            .setId(ID)
            .setPlanNaam("Plan")
            .setGemeente("Middelburg")
            .setProvincie("Zeeland")));
  }

  @Test
  void createsNewPlan() throws Exception {
    loggedInAsGemeente();
    savedPlanIsReturned();
    assertThat(mvc.put().uri(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_PLAN))
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .isLenientlyEqualTo("""
{"id": "%s", "planNaam": "Plan", "provincie": "Zeeland"}""".formatted(ID));
    verify(pmwDb).insertPlanregistratie(any(), anyString());
  }

  @Test
  void replacesExistingPlan() throws Exception {
    loggedInAsGemeente();
    savedPlanIsReturned();
    when(pmwDb.getPlanregistratieGemeente(ID)).thenReturn("Middelburg");
    assertThat(mvc.put().uri(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_PLAN))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.provincie")
        .isEqualTo("Zeeland");
    verify(pmwDb).insertPlanregistratie(any(), anyString());
  }

  @Test
  void notLoggedInIsUnauthorizedBeforeValidation() throws Exception {
    when(authenticationService.requirePlanmonitorUser())
        .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niet ingelogd"));
    assertThat(mvc.put().uri(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .hasStatus(HttpStatus.UNAUTHORIZED);
    verify(pmwDb, never()).insertPlanregistratie(any(), anyString());
  }

  @Test
  void invalidPlanIsRejectedWithErrors() throws Exception {
    loggedInAsGemeente();
    assertThat(mvc.put()
            .uri(URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_PLAN.replace("Herstructurering", "Onbekend")))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.errors[0].field")
        .isEqualTo("plantype");
    verify(pmwDb, never()).insertPlanregistratie(any(), anyString());
  }

  @Test
  void planNaamInUseIsConflict() throws Exception {
    loggedInAsGemeente();
    doThrow(new PlanNaamInUseException("Plan")).when(pmwDb).insertPlanregistratie(any(), anyString());
    assertThat(mvc.put().uri(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_PLAN))
        .hasStatus(HttpStatus.CONFLICT)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Er bestaat al een andere planregistratie met de naam \"Plan\"");
  }

  @Test
  void idInBodyMustMatchPath() {
    loggedInAsGemeente();
    String body = VALID_PLAN.replace("{", "{\"id\": \"" + UUID.randomUUID() + "\",");
    assertThat(mvc.put().uri(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }
}
