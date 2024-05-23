/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

import java.time.OffsetDateTime;

public final class Planregistratie {
  private String id;
  private String geometrie;
  private String creator;
  private OffsetDateTime createdAt;
  private String editor;
  private OffsetDateTime editedAt;
  private String planNaam;
  private String provincie;
  private String gemeente;
  private String regio;
  private String plaatsnaam;
  private String vertrouwelijkheid;
  private String opdrachtgeverType;
  private String opdrachtgeverNaam;
  private String opmerkingen;
  private String plantype;
  private String bestemmingsplan;
  private String statusProject;
  private String statusPlanologisch;
  private String knelpuntenMeerkeuze;
  private String beoogdWoonmilieuAbf13;
  private Integer aantalStudentenwoningen;

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

  public String getKnelpuntenMeerkeuze() {
    return knelpuntenMeerkeuze;
  }

  public Planregistratie setKnelpuntenMeerkeuze(String knelpuntenMeerkeuze) {
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
}
