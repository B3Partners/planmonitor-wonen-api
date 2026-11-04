/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import nl.b3p.planmonitorwonen.api.validation.PlanregistratieValidator;

/**
 * A planregistratie (woningbouwplan). The same model is used for the request body of a PUT and the responses, the
 * fields filled in by the server are read-only.
 */
@Schema(description = """
Een planregistratie (woningbouwplan). Hetzelfde model wordt gebruikt voor de request body van een PUT en de responses.

Verplicht in een PUT zijn `planNaam`, `gemeente`, `geometrie`, `plantype` en `statusPlanologisch`. De overige velden
zijn optioneel: weglaten of `null` betekent niet ingevuld, maar een ingevulde waarde moet geldig zijn. Een PUT
vervangt de hele planregistratie, een weggelaten veld wordt dus leeggemaakt.

De server vult zelf in: `provincie` (op basis van de gemeente), `vertrouwelijkheid` (`Openbaar` als niet opgegeven),
`regio` en `beoogdWoonmilieuAbf13` (op basis van de geometrie als niet opgegeven) en de read-only velden `creator`,
`createdAt`, `editor` en `editedAt`. Read-only velden in een request worden genegeerd.""")
public final class Planregistratie {
  @Schema(
      description =
          "UUID van de planregistratie. Optioneel in de request body, maar indien opgegeven gelijk aan het"
              + " id in het pad",
      format = "uuid",
      example = "0b5c1d0e-3f43-4e4a-9a6e-2f1f6b6d2a10")
  private String id;

  @Schema(
      description = "Begrenzing van het plan als WKT polygoon in RD New (EPSG:28992). Moet een geldige polygoon"
          + " zijn die (deels) in de gemeente ligt",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "POLYGON ((31400 391900, 31600 391900, 31600 392100, 31400 392100, 31400 391900))")
  private String geometrie;

  @Schema(description = "Gebruikersnaam die de planregistratie heeft aangemaakt", accessMode = READ_ONLY)
  private String creator;

  @Schema(description = "Tijdstip van aanmaken", accessMode = READ_ONLY)
  private OffsetDateTime createdAt;

  @Schema(description = "Gebruikersnaam die de planregistratie het laatst heeft opgeslagen", accessMode = READ_ONLY)
  private String editor;

  @Schema(description = "Tijdstip van het laatst opslaan", accessMode = READ_ONLY)
  private OffsetDateTime editedAt;

  @Schema(
      description = "Naam van het plan, uniek voor alle gemeentes",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minLength = PlanregistratieValidator.PLAN_NAAM_MIN_LENGTH,
      example = "Stationsgebied fase 1")
  private String planNaam;

  @Schema(
      description = "Provincie van de gemeente, ingevuld door de server",
      accessMode = READ_ONLY,
      example = "Zeeland")
  private String provincie;

  @Schema(
      description = "Naam van de gemeente, zoals in de Bestuurlijke Gebieden van het Kadaster. De gebruiker moet"
          + " autorisatie hebben voor deze gemeente",
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "Middelburg")
  private String gemeente;

  @Schema(description = "Regio waarin het plan ligt. Als niet opgegeven afgeleid van de geometrie")
  private String regio;

  @Schema(description = "Plaats waarin het plan ligt", example = "Middelburg")
  private String plaatsnaam;

  @Schema(description = "Wie de plangegevens mag zien. Als niet opgegeven `Openbaar`")
  private String vertrouwelijkheid;

  @Schema(description = "Type opdrachtgever")
  private String opdrachtgeverType;

  @Schema(description = "Naam van de opdrachtgever")
  private String opdrachtgeverNaam;

  @Schema(description = "Opmerkingen")
  private String opmerkingen;

  @Schema(description = "Het meest dominante plantype", requiredMode = Schema.RequiredMode.REQUIRED)
  private String plantype;

  @Schema(description = "Bestemmingsplan")
  private String bestemmingsplan;

  @Schema(description = "Projectfase waarin het plan zich bevindt")
  private String statusProject;

  @Schema(description = "Planologische status van het plan", requiredMode = Schema.RequiredMode.REQUIRED)
  private String statusPlanologisch;

  @Schema(description = "Knelpunten bij de realisatie van het plan")
  private String[] knelpuntenMeerkeuze;

  @Schema(
      description =
          "Beoogd woonmilieu volgens de ABF-typologie met 13 woonmilieus. Als niet opgegeven afgeleid van"
              + " de geometrie")
  private String beoogdWoonmilieuAbf13;

  @Schema(description = "Aantal studentenwoningen", minimum = "0")
  private Integer aantalStudentenwoningen;

  @Schema(description = "Of het plan een sleutelproject is")
  private Boolean sleutelproject;

  /** Not included in the JSON output when not loaded. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(
      description = "Aantallen woningen. In een response alleen bij een enkele planregistratie of bij"
          + " `GET /planregistraties?details=true`. Een PUT vervangt alle aantallen, weglaten betekent geen aantallen"
          + " ingevuld")
  private Aantallen aantallen;

  public String getId() {
    return id;
  }

  public Planregistratie setId(String id) {
    this.id = id;
    return this;
  }

  public String getGeometrie() {
    return geometrie;
  }

  public Planregistratie setGeometrie(String geometrie) {
    this.geometrie = geometrie;
    return this;
  }

  public String getCreator() {
    return creator;
  }

  public Planregistratie setCreator(String creator) {
    this.creator = creator;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public Planregistratie setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public String getEditor() {
    return editor;
  }

  public Planregistratie setEditor(String editor) {
    this.editor = editor;
    return this;
  }

  public OffsetDateTime getEditedAt() {
    return editedAt;
  }

  public Planregistratie setEditedAt(OffsetDateTime editedAt) {
    this.editedAt = editedAt;
    return this;
  }

  public String getPlanNaam() {
    return planNaam;
  }

  public Planregistratie setPlanNaam(String planNaam) {
    this.planNaam = planNaam;
    return this;
  }

  public String getProvincie() {
    return provincie;
  }

  public Planregistratie setProvincie(String provincie) {
    this.provincie = provincie;
    return this;
  }

  public String getGemeente() {
    return gemeente;
  }

  public Planregistratie setGemeente(String gemeente) {
    this.gemeente = gemeente;
    return this;
  }

  public String getRegio() {
    return regio;
  }

  public Planregistratie setRegio(String regio) {
    this.regio = regio;
    return this;
  }

  public String getPlaatsnaam() {
    return plaatsnaam;
  }

  public Planregistratie setPlaatsnaam(String plaatsnaam) {
    this.plaatsnaam = plaatsnaam;
    return this;
  }

  public String getVertrouwelijkheid() {
    return vertrouwelijkheid;
  }

  public Planregistratie setVertrouwelijkheid(String vertrouwelijkheid) {
    this.vertrouwelijkheid = vertrouwelijkheid;
    return this;
  }

  public String getOpdrachtgeverType() {
    return opdrachtgeverType;
  }

  public Planregistratie setOpdrachtgeverType(String opdrachtgeverType) {
    this.opdrachtgeverType = opdrachtgeverType;
    return this;
  }

  public String getOpdrachtgeverNaam() {
    return opdrachtgeverNaam;
  }

  public Planregistratie setOpdrachtgeverNaam(String opdrachtgeverNaam) {
    this.opdrachtgeverNaam = opdrachtgeverNaam;
    return this;
  }

  public String getOpmerkingen() {
    return opmerkingen;
  }

  public Planregistratie setOpmerkingen(String opmerkingen) {
    this.opmerkingen = opmerkingen;
    return this;
  }

  public String getPlantype() {
    return plantype;
  }

  public Planregistratie setPlantype(String plantype) {
    this.plantype = plantype;
    return this;
  }

  public String getBestemmingsplan() {
    return bestemmingsplan;
  }

  public Planregistratie setBestemmingsplan(String bestemmingsplan) {
    this.bestemmingsplan = bestemmingsplan;
    return this;
  }

  public String getStatusProject() {
    return statusProject;
  }

  public Planregistratie setStatusProject(String statusProject) {
    this.statusProject = statusProject;
    return this;
  }

  public String getStatusPlanologisch() {
    return statusPlanologisch;
  }

  public Planregistratie setStatusPlanologisch(String statusPlanologisch) {
    this.statusPlanologisch = statusPlanologisch;
    return this;
  }

  public String[] getKnelpuntenMeerkeuze() {
    return knelpuntenMeerkeuze;
  }

  public Planregistratie setKnelpuntenMeerkeuze(String[] knelpuntenMeerkeuze) {
    this.knelpuntenMeerkeuze = knelpuntenMeerkeuze;
    return this;
  }

  public String getBeoogdWoonmilieuAbf13() {
    return beoogdWoonmilieuAbf13;
  }

  public Planregistratie setBeoogdWoonmilieuAbf13(String beoogdWoonmilieuAbf13) {
    this.beoogdWoonmilieuAbf13 = beoogdWoonmilieuAbf13;
    return this;
  }

  public Integer getAantalStudentenwoningen() {
    return aantalStudentenwoningen;
  }

  public Planregistratie setAantalStudentenwoningen(Integer aantalStudentenwoningen) {
    this.aantalStudentenwoningen = aantalStudentenwoningen;
    return this;
  }

  public Boolean getSleutelproject() {
    return sleutelproject;
  }

  public Planregistratie setSleutelproject(Boolean sleutelproject) {
    this.sleutelproject = sleutelproject;
    return this;
  }

  public Aantallen getAantallen() {
    return aantallen;
  }

  public Planregistratie setAantallen(Aantallen aantallen) {
    this.aantallen = aantallen;
    return this;
  }
}
