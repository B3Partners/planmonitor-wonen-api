/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.List;
import java.util.Map;
import nl.b3p.planmonitorwonen.api.service.PlanregistratieAutofillService;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("!test")
public class PlanregistratieAutofillController {
  private final JdbcClient jdbcClient;
  private final PlanregistratieAutofillService autofillService;

  public PlanregistratieAutofillController(JdbcClient jdbcClient, PlanregistratieAutofillService autofillService) {
    this.jdbcClient = jdbcClient;
    this.autofillService = autofillService;
  }

  // See https://www.w3.org/TR/sdw-bp/#applicability-formatVbp, use text/plain content type for WKT
  // in request body
  @PostMapping(
      path = "${planmonitor-wonen-api.base-path}/planregistratie/autofill-by-geometry",
      consumes = "text/plain")
  public Map<String, Object> autofill(@RequestBody String wkt) {
    Geometry geometry;
    try {
      GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 28992);
      geometry = new WKTReader(geometryFactory).read(wkt);
    } catch (ParseException e) {
      throw new ResponseStatusException(BAD_REQUEST, "WKT parsing error");
    }

    List<String> gemeentes = this.jdbcClient
        .sql("""
select g.naam
from gemeente g
where st_intersects(g.geometry, st_geomfromtext(?, 28992))
order by st_area(st_intersection(g.geometry, st_geomfromtext(?, 28992))) desc""")
        .param(wkt)
        .param(wkt)
        .query(String.class)
        .list();

    return Map.of(
        "gemeentes", gemeentes,
        "provincies", autofillService.getProvincies(geometry),
        "regios", autofillService.getRegios(geometry),
        "woonmilieus", autofillService.getWoonmilieus(geometry));
  }
}
