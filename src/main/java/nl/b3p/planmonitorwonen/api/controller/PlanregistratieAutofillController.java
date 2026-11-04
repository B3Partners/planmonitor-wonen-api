/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static nl.b3p.planmonitorwonen.api.configuration.ApiVersionConfiguration.MAJOR_VERSION;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.FORBIDDEN;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.UNAUTHORIZED;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.WEBAPP_TAG;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.WEBAPP_TAG_DESCRIPTION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "${planmonitor-wonen-api.base-path}/{version}", version = MAJOR_VERSION)
@Profile("!test")
@Tag(name = WEBAPP_TAG, description = WEBAPP_TAG_DESCRIPTION)
public class PlanregistratieAutofillController {
  private final JdbcClient jdbcClient;
  private final PlanregistratieAutofillService autofillService;
  private final PlanmonitorAuthenticationService authenticationService;

  public PlanregistratieAutofillController(
      JdbcClient jdbcClient,
      PlanregistratieAutofillService autofillService,
      PlanmonitorAuthenticationService authenticationService) {
    this.jdbcClient = jdbcClient;
    this.autofillService = autofillService;
    this.authenticationService = authenticationService;
  }

  // See https://www.w3.org/TR/sdw-bp/#applicability-formatVbp, use text/plain content type for WKT
  // in request body
  @PostMapping(path = "/planregistratie/autofill-by-geometry", consumes = "text/plain")
  @Operation(
      summary = "Gemeentes, provincies, regio's en woonmilieus die een geometrie snijden",
      description = "De request body is een geometrie als WKT in RD New (EPSG:28992). De gemeentes zijn"
          + " gesorteerd op de grootte van de overlap, de grootste eerst. Voor het vooraf invullen van het"
          + " formulier van de webapplicatie, een client heeft dit niet nodig: de server vult `provincie`, `regio`"
          + " en `beoogdWoonmilieuAbf13` bij het opslaan zelf in.")
  @ApiResponse(responseCode = "200", description = "De gevonden waarden per veld")
  @ApiResponse(responseCode = "400", description = "Ongeldige WKT")
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED)
  @ApiResponse(responseCode = "403", ref = FORBIDDEN)
  public Map<String, Object> autofill(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = "text/plain",
                      schema =
                          @Schema(
                              example =
                                  "POLYGON ((31400 391900, 31600 391900, 31600 392100, 31400 392100, 31400 391900))")))
          @RequestBody
          String wkt) {
    // Only for users who can edit plans, this runs spatial queries on an arbitrary geometry
    authenticationService.requirePlanmonitorUser();

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
