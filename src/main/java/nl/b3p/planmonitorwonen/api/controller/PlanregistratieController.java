/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static nl.b3p.planmonitorwonen.api.configuration.ApiVersionConfiguration.MAJOR_VERSION;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.FORBIDDEN;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.NOT_FOUND;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.PLANREGISTRATIE_EXAMPLE;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.PROBLEM_JSON;
import static nl.b3p.planmonitorwonen.api.configuration.OpenApiConfiguration.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.UUID;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import nl.b3p.planmonitorwonen.api.model.ProblemDetailSchema;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import nl.b3p.planmonitorwonen.api.service.PlanregistratieCompletionService;
import nl.b3p.planmonitorwonen.api.validation.EnumValues;
import nl.b3p.planmonitorwonen.api.validation.PlanNaamInUseException;
import nl.b3p.planmonitorwonen.api.validation.PlanregistratieValidationException;
import nl.b3p.planmonitorwonen.api.validation.PlanregistratieValidator;
import nl.b3p.planmonitorwonen.api.validation.ValidationError;
import org.locationtech.jts.io.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "${planmonitor-wonen-api.base-path}/{version}", version = MAJOR_VERSION)
@Profile("!test")
@Tag(name = "Planregistraties", description = "Opvragen, aanleveren en verwijderen van planregistraties")
public class PlanregistratieController {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PlanmonitorAuthenticationService authenticationService;
  private final PlanmonitorWonenDatabaseService pmwDb;
  private final PlanregistratieCompletionService completionService;
  private final EnumValues enumValues;

  public PlanregistratieController(
      PlanmonitorAuthenticationService authenticationService,
      PlanmonitorWonenDatabaseService planmonitorWonenDatabaseService,
      PlanregistratieCompletionService completionService,
      EnumValues enumValues) {
    this.authenticationService = authenticationService;
    this.pmwDb = planmonitorWonenDatabaseService;
    this.completionService = completionService;
    this.enumValues = enumValues;
  }

  @GetMapping(path = "/planregistraties")
  @Operation(
      summary = "Alle planregistraties die de gebruiker mag inzien",
      description = "Voor een gemeentegebruiker de planregistraties van de gemeentes waarvoor de gebruiker"
          + " autorisatie heeft, voor een provinciegebruiker de planregistraties van de provincie.")
  @ApiResponse(responseCode = "200", description = "De planregistraties")
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED)
  @ApiResponse(responseCode = "403", ref = FORBIDDEN)
  public List<Planregistratie> planregistraties(
      @Parameter(description = "Ook de aantallen van de planregistraties teruggeven")
          @RequestParam(required = false)
          boolean details) {
    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        authenticationService.requirePlanmonitorUser();
    if (auth.isProvincie()) {
      return pmwDb.getPlanregistratiesForProvincie(details);
    } else {
      return pmwDb.getPlanregistratiesForGemeentes(auth.gemeentes(), details);
    }
  }

  @GetMapping(path = "/planregistratie/{id}")
  @Operation(summary = "Een planregistratie met de aantallen")
  @ApiResponse(responseCode = "200", description = "De planregistratie")
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED)
  @ApiResponse(responseCode = "403", ref = FORBIDDEN)
  @ApiResponse(responseCode = "404", ref = NOT_FOUND)
  public Planregistratie planregistratie(
      @Parameter(description = "UUID van de planregistratie") @PathVariable("id") UUID id) {
    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        authenticationService.requirePlanmonitorUser();

    Planregistratie planregistratie = pmwDb.getPlanregistratie(id.toString())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!auth.isProvincie() && !auth.gemeentes().contains(planregistratie.getGemeente())) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to access plan id {} of gemeente {}, denied",
          auth.authentication().getName(),
          auth.gemeentes(),
          id,
          planregistratie.getGemeente());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return planregistratie;
  }

  /**
   * Creates or replaces a planregistratie. Returns the saved planregistratie including the fields filled in by the
   * server, with status 201 when it was created and 200 when it was replaced.
   */
  @PutMapping(path = "/planregistratie/{id}")
  @Operation(summary = "Een planregistratie aanmaken of vervangen", description = """
Maakt een planregistratie aan met het id in het pad, of vervangt de bestaande planregistratie met dat id in zijn
geheel, inclusief de aantallen. Het id wordt door de client gekozen.

De response bevat de opgeslagen planregistratie, inclusief de velden die door de server zijn ingevuld.""")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              schema = @Schema(implementation = Planregistratie.class),
              examples = @ExampleObject(name = "Planregistratie", ref = PLANREGISTRATIE_EXAMPLE)))
  @ApiResponse(
      responseCode = "200",
      description = "De bestaande planregistratie is vervangen",
      content = @Content(schema = @Schema(implementation = Planregistratie.class)))
  @ApiResponse(
      responseCode = "201",
      description = "De planregistratie is aangemaakt",
      content = @Content(schema = @Schema(implementation = Planregistratie.class)))
  @ApiResponse(
      responseCode = "400",
      description =
          "Ongeldige planregistratie, met de fouten per veld in `errors`. Ook bij ongeldige JSON, een"
              + " ongeldig UUID of een id in de body dat niet gelijk is aan het id in het pad, dan zonder `errors`",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailSchema.class)))
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED)
  @ApiResponse(
      responseCode = "403",
      description = "Geen toegang: de gebruiker heeft geen autorisatie voor Planmonitor Wonen of voor de gemeente"
          + " van de planregistratie, de bestaande planregistratie met dit id is van een andere gemeente, of de"
          + " gebruiker is een provinciegebruiker (alleen lezen)",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailSchema.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Er bestaat al een andere planregistratie met dezelfde plannaam (uniek voor alle gemeentes),"
          + " met een fout voor `planNaam` in `errors`",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailSchema.class)))
  public ResponseEntity<Planregistratie> put(
      @Parameter(description = "UUID van de planregistratie") @PathVariable("id") UUID id,
      @RequestBody Planregistratie planregistratie)
      throws ParseException {

    // Authenticate before validating, so a client which is not logged in gets a 401 instead of a 400
    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        authenticationService.requirePlanmonitorUser();

    if (planregistratie.getId() != null && !isSameUuid(id, planregistratie.getId())) {
      throw new ResponseStatusException(BAD_REQUEST, "Id in de body komt niet overeen met het id in het pad");
    }
    planregistratie.setId(id.toString());

    List<ValidationError> errors = PlanregistratieValidator.validate(planregistratie, enumValues);
    if (!errors.isEmpty()) {
      logRejected(auth, planregistratie, errors);
      throw new PlanregistratieValidationException(errors);
    }

    if (auth.isProvincie()) {
      logger.warn(
          "Provincie user \"{}\" tried to save plan id {}, denied",
          auth.authentication().getName(),
          id);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    if (!auth.gemeentes().contains(planregistratie.getGemeente())) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to save plan id {}, name \"{}\" with gemeente value {}, denied",
          auth.authentication().getName(),
          auth.gemeentes(),
          id,
          planregistratie.getPlanNaam(),
          planregistratie.getGemeente());
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    String gemeente = pmwDb.getPlanregistratieGemeente(id.toString());

    if (gemeente != null && !auth.isProvincie() && !auth.gemeentes().contains(gemeente)) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to update plan id {}, name \"{}\" of gemeente {}, denied",
          auth.authentication().getName(),
          auth.gemeentes(),
          id,
          planregistratie.getPlanNaam(),
          gemeente);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    try {
      completionService.complete(planregistratie);
    } catch (PlanregistratieValidationException e) {
      logRejected(auth, planregistratie, e.getErrors());
      throw e;
    }
    boolean created = gemeente == null;
    try {
      pmwDb.insertPlanregistratie(planregistratie, auth.authentication().getName());
    } catch (PlanNaamInUseException e) {
      logger.info(
          "User \"{}\" tried to save plan id {} with name \"{}\" already in use by another plan, rejected",
          auth.authentication().getName(),
          id,
          planregistratie.getPlanNaam());
      throw e;
    }
    Planregistratie saved = pmwDb.getPlanregistratie(id.toString())
        .orElseThrow(() -> new IllegalStateException("Saved planregistratie " + id + " not found"));
    return ResponseEntity.status(created ? CREATED : OK).body(saved);
  }

  @DeleteMapping(path = "/planregistratie/{id}")
  @Operation(summary = "Een planregistratie verwijderen")
  @ApiResponse(responseCode = "200", description = "De planregistratie is verwijderd")
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED)
  @ApiResponse(
      responseCode = "403",
      description = "Geen toegang: de gebruiker heeft geen autorisatie voor Planmonitor Wonen of voor de gemeente"
          + " van de planregistratie, of de gebruiker is een provinciegebruiker (alleen lezen)",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailSchema.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Er is geen planregistratie met dit id. Een client kan dit behandelen als al verwijderd",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailSchema.class)))
  public ResponseEntity<Void> delete(
      @Parameter(description = "UUID van de planregistratie") @PathVariable("id") UUID id) {
    PlanmonitorAuthenticationService.PlanmonitorAuthentication auth =
        authenticationService.requirePlanmonitorUser();

    if (auth.isProvincie()) {
      logger.warn(
          "Provincie user \"{}\" tried to delete plan id {}, denied",
          auth.authentication().getName(),
          id);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    String gemeente = pmwDb.getPlanregistratieGemeente(id.toString());

    if (gemeente == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    if (!auth.gemeentes().contains(gemeente)) {
      logger.warn(
          "Gemeente user \"{}\" with authorization for gemeentes {} tried to delete plan id {} of gemeente {}, denied",
          auth.authentication().getName(),
          auth.gemeentes(),
          id,
          gemeente);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    pmwDb.deletePlanregistratie(id.toString());
    return ResponseEntity.status(OK).build();
  }

  private static void logRejected(
      PlanmonitorAuthenticationService.PlanmonitorAuthentication auth,
      Planregistratie planregistratie,
      List<ValidationError> errors) {
    logger.info(
        "User \"{}\" tried to save invalid plan id {}, name \"{}\", rejected with {} error(s): {}",
        auth.authentication().getName(),
        planregistratie.getId(),
        planregistratie.getPlanNaam(),
        errors.size(),
        errors);
  }

  private static boolean isSameUuid(UUID id, String other) {
    try {
      return id.equals(UUID.fromString(other));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
