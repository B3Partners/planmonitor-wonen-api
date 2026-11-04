/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import javax.sql.DataSource;
import nl.b3p.planmonitorwonen.api.configuration.JdbcSessionConfiguration;
import nl.b3p.planmonitorwonen.api.model.Aantal;
import nl.b3p.planmonitorwonen.api.model.Aantallen;
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
  private static final OffsetDateTime CLIENT_TIMESTAMP = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

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
    Aantallen aantallen = aantallen(aantal(10, Map.of(2025, 10)), Map.of());
    pmwDb.insertPlanregistratie(planregistratie(id, "Plan transactie").setAantallen(aantallen), "gemeente");

    // Invalid enum value, rejected by the database after the existing plan has been deleted
    Planregistratie updated = planregistratie(id, "Plan transactie gewijzigd")
        .setAantallen(aantallen(aantal(10, Map.of()), Map.of("Ongeldig", aantal(10, Map.of()))));
    assertThrows(DataAccessException.class, () -> pmwDb.insertPlanregistratie(updated, "gemeente"));

    Planregistratie stored = getPlanregistratie(id);
    assertEquals("Plan transactie", stored.getPlanNaam());
    assertEquals(aantallen, stored.getAantallen());
  }

  @Test
  void insertAndReadAantallen() throws Exception {
    String id = UUID.randomUUID().toString();
    Aantallen aantallen = new Aantallen(
        aantal(10, Map.of(2025, 4, 2026, 6)),
        aantal(2, Map.of(2025, 2)),
        Map.of("Eengezins", aantal(4, Map.of(2025, 4)), "Meergezins", aantal(6, Map.of(2026, 6))),
        Map.of(),
        Map.of("Regulier permanent", aantal(10, Map.of(2025, 4, 2026, 6))),
        Map.of("Sociale huur", aantal(10, Map.of(2025, 4, 2026, 6))));
    pmwDb.insertPlanregistratie(planregistratie(id, "Plan aantallen").setAantallen(aantallen), "gemeente");

    assertEquals(aantallen, getPlanregistratie(id).getAantallen());

    Planregistratie withoutAantallen = pmwDb.getPlanregistratiesForGemeentes(Set.of("Middelburg"), false).stream()
        .filter(p -> p.getId().equals(id))
        .findFirst()
        .orElseThrow();
    assertNull(withoutAantallen.getAantallen());

    Planregistratie withAantallen = pmwDb.getPlanregistratiesForProvincie(true).stream()
        .filter(p -> p.getId().equals(id))
        .findFirst()
        .orElseThrow();
    assertEquals(aantallen, withAantallen.getAantallen());
  }

  @Test
  void updateReplacesAantallen() throws Exception {
    String id = UUID.randomUUID().toString();
    pmwDb.insertPlanregistratie(
        planregistratie(id, "Plan vervangen")
            .setAantallen(aantallen(
                aantal(10, Map.of(2025, 10)), Map.of("Eengezins", aantal(10, Map.of(2025, 10))))),
        "gemeente");

    Aantallen replacement = aantallen(aantal(10, Map.of(2030, 10)), Map.of());
    pmwDb.insertPlanregistratie(planregistratie(id, "Plan vervangen").setAantallen(replacement), "gemeente");

    assertEquals(replacement, getPlanregistratie(id).getAantallen());
  }

  @Test
  void missingAantallenSavesPlanWithoutAantallen() throws Exception {
    String id = UUID.randomUUID().toString();
    pmwDb.insertPlanregistratie(planregistratie(id, "Plan zonder aantallen"), "gemeente");

    assertEquals(aantallen(null, Map.of()), getPlanregistratie(id).getAantallen());
  }

  @Test
  void databaseRejectsDuplicatePlancategorie() throws Exception {
    String id = UUID.randomUUID().toString();
    pmwDb.insertPlanregistratie(
        planregistratie(id, "Plan dubbel").setAantallen(aantallen(aantal(10, Map.of()), Map.of())), "gemeente");

    assertThrows(DataAccessException.class, () -> jdbcClient
        .sql("insert into plancategorie(planregistratie_id, nieuwbouw) values (?::uuid, 'Nieuwbouw')")
        .param(id)
        .update());
    assertThrows(DataAccessException.class, () -> jdbcClient
        .sql("insert into plancategorie(planregistratie_id, nieuwbouw, sloop) values (?::uuid, null, null)")
        .param(id)
        .update());
  }

  @Test
  void provincieForGemeente() {
    assertEquals(Optional.of("Zeeland"), pmwDb.getProvincieForGemeente("Middelburg"));
    assertEquals(Optional.empty(), pmwDb.getProvincieForGemeente("Onbekend"));
  }

  @Test
  void insertSetsCreatorAndIgnoresClientValues() throws Exception {
    String id = UUID.randomUUID().toString();
    pmwDb.insertPlanregistratie(planregistratie(id, "Plan nieuw"), "gemeente");

    Planregistratie stored = getPlanregistratie(id);
    assertEquals("gemeente", stored.getCreator());
    assertTrue(stored.getCreatedAt().isAfter(CLIENT_TIMESTAMP));
    assertNull(stored.getEditor());
    assertNull(stored.getEditedAt());
  }

  @Test
  void updateKeepsCreatorAndSetsEditor() throws Exception {
    String id = UUID.randomUUID().toString();
    pmwDb.insertPlanregistratie(planregistratie(id, "Plan bijwerken"), "gemeente");
    Planregistratie inserted = getPlanregistratie(id);

    pmwDb.insertPlanregistratie(planregistratie(id, "Plan bijwerken"), "andere gebruiker");

    Planregistratie updated = getPlanregistratie(id);
    assertEquals("gemeente", updated.getCreator());
    assertEquals(inserted.getCreatedAt().toInstant(), updated.getCreatedAt().toInstant());
    assertEquals("andere gebruiker", updated.getEditor());
    assertNotNull(updated.getEditedAt());
    assertTrue(updated.getEditedAt().isAfter(CLIENT_TIMESTAMP));
  }

  private Planregistratie getPlanregistratie(String id) {
    return pmwDb.getPlanregistratie(id).orElseThrow(() -> new AssertionError("Planregistratie not found"));
  }

  private static Planregistratie planregistratie(String id, String planNaam) {
    return new Planregistratie()
        .setId(id)
        .setGeometrie("POLYGON ((30000 390000, 31000 390000, 31000 391000, 30000 391000, 30000 390000))")
        // Values from the client, must be ignored
        .setCreator("client")
        .setCreatedAt(CLIENT_TIMESTAMP)
        .setEditor("client")
        .setEditedAt(CLIENT_TIMESTAMP)
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

  private static Aantal aantal(int gepland, Map<Integer, Integer> perJaar) {
    return new Aantal(gepland, 0, new TreeMap<>(perJaar));
  }

  private static Aantallen aantallen(Aantal bruto, Map<String, Aantal> woningType) {
    return new Aantallen(bruto, null, woningType, Map.of(), Map.of(), Map.of());
  }
}
