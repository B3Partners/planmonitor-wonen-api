/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import nl.b3p.planmonitorwonen.api.configuration.JdbcSessionConfiguration;
import nl.b3p.planmonitorwonen.api.model.Detailplanning;
import nl.b3p.planmonitorwonen.api.model.Plancategorie;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = {JdbcSessionConfiguration.class, PlanmonitorWonenDatabaseService.class},
    properties = "spring.flyway.enabled=true")
@ImportAutoConfiguration({
  FlywayAutoConfiguration.class,
  DataSourceTransactionManagerAutoConfiguration.class,
  TransactionAutoConfiguration.class
})
@ActiveProfiles("integration-test")
@Testcontainers
class PlanmonitorWonenDatabaseServiceTest {
  @Container
  static final PostgreSQLContainer postgis = new PostgreSQLContainer(
      DockerImageName.parse("postgis/postgis:18-3.6-alpine").asCompatibleSubstituteFor("postgres"));

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    // The Tailormap session database is only read from, use the same database for the test
    for (String prefix : List.of("spring.datasource", "tailormap.datasource")) {
      registry.add(prefix + ".url", postgis::getJdbcUrl);
      registry.add(prefix + ".username", postgis::getUsername);
      registry.add(prefix + ".password", postgis::getPassword);
    }
  }

  @Autowired
  private PlanmonitorWonenDatabaseService pmwDb;

  @Autowired
  private JdbcClient jdbcClient;

  @Autowired
  private DataSource dataSource;

  @Autowired
  @Qualifier("tailormapDataSource") private DataSource tailormapDataSource;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private JdbcIndexedSessionRepository sessionRepository;

  @BeforeEach
  void insertGemeente() {
    jdbcClient.sql("""
insert into gemeente(identificatie, naam, provincie, geometry)
values ('GM0687', 'Middelburg', 'Zeeland', ST_GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 28992))
on conflict do nothing""").update();
  }

  @Test
  void defaultTransactionManagerUsesPlanmonitorWonenDataSource() {
    assertSame(dataSource, ((DataSourceTransactionManager) transactionManager).getDataSource());
  }

  @Test
  void sessionRepositoryUsesTailormapDataSource() {
    TransactionTemplate sessionTransactionTemplate =
        (TransactionTemplate) ReflectionTestUtils.getField(sessionRepository, "transactionOperations");
    assertSame(
        tailormapDataSource,
        ((DataSourceTransactionManager) sessionTransactionTemplate.getTransactionManager()).getDataSource());
  }

  @Test
  void failedUpdateKeepsExistingPlanregistratie() throws Exception {
    String id = UUID.randomUUID().toString();
    Planregistratie planregistratie = planregistratie(id, "Plan transactie");
    Plancategorie nieuwbouw = plancategorie(id, "Nieuwbouw", null);
    pmwDb.insertPlanregistratie(
        planregistratie, List.of(nieuwbouw), List.of(detailplanning(nieuwbouw.id(), 2025, 10)));

    Planregistratie updated = planregistratie(id, "Plan transactie gewijzigd");
    // Invalid enum value, rejected by the database after the existing plan has been deleted
    Plancategorie invalid = plancategorie(id, "Nieuwbouw", "Ongeldig");
    assertThrows(
        DataAccessException.class, () -> pmwDb.insertPlanregistratie(updated, List.of(invalid), List.of()));

    Planregistratie stored = pmwDb.getPlanregistratiesForGemeentes(Set.of("Middelburg")).stream()
        .filter(p -> p.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Existing planregistratie deleted by failed update"));
    assertEquals("Plan transactie", stored.getPlanNaam());
    assertEquals(1, pmwDb.getPlancategorieen(id).size());
    assertEquals(1, pmwDb.getDetailplanningen(id).size());
  }

  private static Planregistratie planregistratie(String id, String planNaam) {
    return new Planregistratie()
        .setId(id)
        .setGeometrie("POLYGON ((30000 390000, 31000 390000, 31000 391000, 30000 391000, 30000 390000))")
        .setCreator("test")
        .setCreatedAt(OffsetDateTime.now(ZoneId.of("Europe/Amsterdam")))
        .setPlanNaam(planNaam)
        .setProvincie("Zeeland")
        .setGemeente("Middelburg")
        .setRegio("Walcheren")
        .setPlaatsnaam("Middelburg")
        .setVertrouwelijkheid("Openbaar")
        .setOpdrachtgeverType("Gemeente")
        .setPlantype("Herstructurering")
        .setStatusProject("Voorbereiding")
        .setStatusPlanologisch("3. In voorbereiding")
        .setKnelpuntenMeerkeuze(new String[] {"Bereikbaarheid"})
        .setBeoogdWoonmilieuAbf13("Centrum")
        .setAantalStudentenwoningen(0)
        .setSleutelproject(false);
  }

  private static Plancategorie plancategorie(String planregistratieId, String nieuwbouw, String woningType) {
    return new Plancategorie(
        UUID.randomUUID().toString(),
        planregistratieId,
        null,
        null,
        null,
        null,
        nieuwbouw,
        woningType,
        null,
        null,
        null,
        null,
        10,
        0);
  }

  private static Detailplanning detailplanning(String plancategorieId, int jaartal, int aantalGepland) {
    return new Detailplanning(
        UUID.randomUUID().toString(), plancategorieId, null, null, null, null, jaartal, aantalGepland);
  }
}
