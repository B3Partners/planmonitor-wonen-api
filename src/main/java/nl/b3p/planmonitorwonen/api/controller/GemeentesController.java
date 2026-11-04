/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static nl.b3p.planmonitorwonen.api.configuration.ApiVersionConfiguration.MAJOR_VERSION;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.UNAUTHORIZED;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.WEBAPP_TAG;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.WEBAPP_TAG_DESCRIPTION;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import nl.b3p.planmonitorwonen.api.model.Gemeente;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "${planmonitor-wonen-api.base-path}/{version}", version = MAJOR_VERSION)
@Profile("!test")
@Tag(name = WEBAPP_TAG, description = WEBAPP_TAG_DESCRIPTION)
public class GemeentesController {
  private final JdbcClient jdbcClient;
  private final PlanmonitorAuthenticationService authenticationService;

  public GemeentesController(JdbcClient jdbcClient, PlanmonitorAuthenticationService authenticationService) {
    this.jdbcClient = jdbcClient;
    this.authenticationService = authenticationService;
  }

  @GetMapping(path = "/gemeentes")
  @Operation(
      summary = "Alle gemeentes, gesorteerd op naam",
      description = "Voor de keuzelijsten van de webapplicatie, een client heeft dit niet nodig.")
  @ApiResponse(responseCode = "200", description = "De gemeentes")
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED)
  public ResponseEntity<List<Gemeente>> planregistraties(
      @Parameter(description = "Alleen de gemeentes van deze provincie", example = "Zeeland")
          @RequestParam(required = false)
          String provincie) {
    // Any logged-in user, a Tailormap admin needs the gemeentes for the group properties
    authenticationService.requireAuthenticated();

    JdbcClient.StatementSpec statementSpec;
    if (provincie == null) {
      statementSpec = jdbcClient.sql("select identificatie, naam, provincie from gemeente order by naam");
    } else {
      statementSpec = jdbcClient
          .sql("select identificatie, naam, provincie from gemeente where provincie = ? order by naam")
          .param(provincie);
    }
    List<Gemeente> gemeentes = statementSpec.query(Gemeente.class).list();
    return ResponseEntity.ok(gemeentes);
  }
}
