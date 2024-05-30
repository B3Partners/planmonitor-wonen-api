/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import java.util.List;
import nl.b3p.planmonitorwonen.api.model.Gemeente;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
public class GemeentesController {
  private final JdbcClient jdbcClient;

  public GemeentesController(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/gemeentes")
  public ResponseEntity<List<Gemeente>> planregistraties(
      @RequestParam(required = false) String provincie) {
    JdbcClient.StatementSpec statementSpec;
    if (provincie == null) {
      statementSpec =
          jdbcClient.sql("select identificatie, naam, provincie from gemeente order by naam");
    } else {
      statementSpec =
          jdbcClient
              .sql(
                  "select identificatie, naam, provincie from gemeente where provincie = ? order by naam")
              .param(provincie);
    }
    List<Gemeente> gemeentes = statementSpec.query(Gemeente.class).list();
    return ResponseEntity.ok(gemeentes);
  }
}
