/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import nl.b3p.planmonitorwonen.api.ImportGemeentesApplication;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Detailplanning;
import nl.b3p.planmonitorwonen.api.model.Plancategorie;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.locationtech.jts.io.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
@ConditionalOnProperty(name = "planmonitor-wonen-api.populate-testdata", havingValue = "true")
@Profile("!test")
public class PopulateTestData {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PlanmonitorWonenDatabaseService pmwDb;

  private final JdbcClient jdbcClient;

  public PopulateTestData(PlanmonitorWonenDatabaseService pmwDb, JdbcClient jdbcClient) {
    this.pmwDb = pmwDb;
    this.jdbcClient = jdbcClient;
  }

  @PostConstruct
  public void init() throws ParseException {
    if (!pmwDb.getPlanregistraties().isEmpty()) {
      return;
    }

    boolean gemeentesLoaded = false;
    try {
      logger.info("Laden gemeentes WFS...");
      populateGemeentes();
      logger.info(
          "Succes, {} gemeentes geladen",
          this.jdbcClient.sql("select count(*) from gemeente").query().singleValue());
      gemeentesLoaded = true;
    } catch (Exception e) {
      logger.error("Fout bij laden van gemeentes", e);
    }

    String wkt =
        "POLYGON ((27791.55972066056 397785.16112066095, 24697.205902478643 393790.63164628064, 26778.86210743738 391483.93152727233, 30548.347667768074 395084.6341520658, 30548.347667768074 395084.6341520658, 27791.55972066056 397785.16112066095))";

    Planregistratie planregistratie =
        new Planregistratie()
            .setId(UUID.randomUUID().toString())
            .setGeometrie(wkt)
            .setCreator("test")
            .setCreatedAt(OffsetDateTime.now(ZoneId.of("Europe/Amsterdam")))
            .setPlanNaam("Plan 1")
            .setProvincie("Zeeland")
            .setGemeente(gemeentesLoaded ? "Middelburg (Z.)" : null)
            .setRegio("Walcheren")
            .setPlaatsnaam("Middelburg")
            .setVertrouwelijkheid("Openbaar")
            .setOpdrachtgeverType("Gemeente")
            .setOpdrachtgeverNaam("De opdrachtgever")
            .setOpmerkingen("Opmerking\nTweede regel")
            .setPlantype("Herstructurering")
            .setBestemmingsplan("Een bestemmingsplan")
            .setStatusProject("Voorbereiding")
            .setStatusPlanologisch("3. In voorbereiding")
            .setKnelpuntenMeerkeuze("Bereikbaarheid")
            .setBeoogdWoonmilieuAbf13("Centrum-dorps")
            .setAantalStudentenwoningen(70)
            .setSleutelproject(false);
    Plancategorie[] plancategorieen = {
      new Plancategorie(
          UUID.randomUUID().toString(),
          planregistratie.getId(),
          null,
          null,
          null,
          null,
          "Nieuwbouw",
          null,
          null,
          null,
          null,
          null,
          20,
          0)
    };
    Detailplanning[] detailplanningen = {
      new Detailplanning(
          UUID.randomUUID().toString(), plancategorieen[0].id(), null, null, null, null, 2025, 10),
      new Detailplanning(
          UUID.randomUUID().toString(), plancategorieen[0].id(), null, null, null, null, 2028, 10)
    };
    pmwDb.insertPlanregistratie(planregistratie, plancategorieen, detailplanningen);
  }

  @Value("${planmonitor-wonen-api.wfs.bestuurlijke-gebieden}")
  private String bestuurlijkeGebiedenWfs;

  @Value("${planmonitor-wonen-api.wfs.bestuurlijke-gebieden.gemeentes-typename}")
  private String gemeentesTypename;

  private void populateGemeentes() throws IOException {
    String sql =
        ImportGemeentesApplication.getGemeentesSql(bestuurlijkeGebiedenWfs, gemeentesTypename);
    this.jdbcClient.sql(sql).update();
  }
}
