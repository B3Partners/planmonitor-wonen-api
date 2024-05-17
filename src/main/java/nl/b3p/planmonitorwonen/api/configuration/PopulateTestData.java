/*
 * Copyright (C) 2024 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import jakarta.annotation.PostConstruct;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.model.Detailplanning;
import nl.b3p.planmonitorwonen.api.model.Plancategorie;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.locationtech.jts.io.ParseException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@Configuration
@ConditionalOnProperty(name = "planmonitor-wonen-api.populate-testdata", havingValue = "true")
@Profile("!test")
public class PopulateTestData {

  private final PlanmonitorWonenDatabaseService pmwDb;

  public PopulateTestData(PlanmonitorWonenDatabaseService pmwDb) {
    this.pmwDb = pmwDb;
  }

  @PostConstruct
  public void init() throws ParseException {
    if (!pmwDb.getPlanregistraties().isEmpty()) {
      return;
    }

    String wkt =
        "POLYGON ((27791.55972066056 397785.16112066095, 24697.205902478643 393790.63164628064, 26778.86210743738 391483.93152727233, 30548.347667768074 395084.6341520658, 30548.347667768074 395084.6341520658, 27791.55972066056 397785.16112066095))";

    String id = UUID.randomUUID().toString();
    pmwDb.insertPlanregistratie(
        new Planregistratie()
            .setId(id)
            .setGeometrie(wkt)
            .setCreator("test")
            .setPlanNaam("Plan 1")
            .setProvincie("Zeeland")
            .setGemeente("Middelburg")
            .setRegio("Walcheren")
            .setPlaatsnaam("Middelburg")
            .setVertrouwelijkheid("Openbaar")
            .setOpdrachtgeverType("Gemeente")
            .setOpdrachtgeverNaam("De opdrachtgever")
            .setJaarStartProject(2026)
            .setOpleveringEerste(2027)
            .setOpleveringLaatste(2030)
            .setOpmerkingen("Opmerking\nTweede regel")
            .setPlantype("Herstructurering")
            .setBestemmingsplan("Een bestemmingsplan")
            .setStatusProject("Voorbereiding")
            .setStatusPlanologisch("3. In voorbereiding")
            .setKnelpuntenMeerkeuze("Bereikbaarheid")
            .setRegionalePlanlijst("Koopwoning")
            .setToelichtingKnelpunten("Herstructurering")
            .setFlexwoningen(50)
            .setLevensloopbestendigJa(10)
            .setLevensloopbestendigNee(40)
            .setBeoogdWoonmilieuAbf5("Buitencentrum")
            .setBeoogdWoonmilieuAbf13("Centrum-dorps")
            .setAantalStudentenwoningen(70)
            .setToelichtingKwalitatief("Kwalitatieve toelichting"));
    String cId = UUID.randomUUID().toString();
    pmwDb.insertOrReplacePlandetails(
        id,
        new Plancategorie[] {
          new Plancategorie(
              cId, id, null, null, null, null, "Nieuwbouw", null, null, null, null, null, 20, 0)
        },
        new Detailplanning[] {
          new Detailplanning(UUID.randomUUID().toString(), cId, null, null, null, null, 2025, 10),
          new Detailplanning(UUID.randomUUID().toString(), cId, null, null, null, null, 2028, 10)
        });
  }
}
