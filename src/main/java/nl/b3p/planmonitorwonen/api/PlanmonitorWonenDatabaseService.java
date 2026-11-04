/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api;

import static java.util.stream.Collectors.groupingBy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import nl.b3p.planmonitorwonen.api.AantallenRows.DetailplanningRow;
import nl.b3p.planmonitorwonen.api.AantallenRows.NewPlancategorie;
import nl.b3p.planmonitorwonen.api.AantallenRows.PlancategorieRow;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.jspecify.annotations.NonNull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.postgresql.jdbc.PgArray;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SimplePropertyRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class PlanmonitorWonenDatabaseService {
  private final JdbcClient jdbcClient;
  private final RowMapper<Planregistratie> planregistratieRowMapper;

  public PlanmonitorWonenDatabaseService(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;

    final GenericConversionService conversionService = new GenericConversionService();
    DefaultConversionService.addDefaultConverters(conversionService);
    conversionService.addConverter(new Converter<PgArray, String[]>() {
      @Override
      public String[] convert(PgArray source) {
        try {
          return (String[]) source.getArray();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
    });

    planregistratieRowMapper = new SimplePropertyRowMapper<>(Planregistratie.class, conversionService) {
      @Override
      @NonNull public Planregistratie mapRow(@NonNull ResultSet rs, int rowNumber) throws SQLException {
        Planregistratie planregistratie = super.mapRow(rs, rowNumber);
        planregistratie.setGeometrie(wkbToWkt(planregistratie.getGeometrie()));
        return planregistratie;
      }
    };
  }

  private String wkbToWkt(String wkb) {
    if (wkb == null) {
      return null;
    }
    try {
      Geometry geometry = new WKBReader().read(WKBReader.hexToBytes(wkb));
      return new WKTWriter().write(geometry);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] wktToWkb(String wkt) throws ParseException {
    Geometry geometry = new WKTReader().read(wkt);
    return new WKBWriter().write(geometry);
  }

  private String sqlQuestionMarks(int count) {
    String[] qs = new String[count];
    Arrays.fill(qs, "?");
    return String.join(", ", qs);
  }

  /**
   * Returns all planregistraties, with the aantallen if requested. Uses a repeatable read transaction so the
   * aantallen are consistent with the planregistraties.
   */
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public List<Planregistratie> getPlanregistratiesForProvincie(boolean withAantallen) {
    return getPlanregistraties("true", Map.of(), withAantallen);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public List<Planregistratie> getPlanregistratiesForGemeentes(Collection<String> gemeentes, boolean withAantallen) {
    return getPlanregistraties("gemeente in (:gemeentes)", Map.of("gemeentes", gemeentes), withAantallen);
  }

  /** Returns a planregistratie with its aantallen. */
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Optional<Planregistratie> getPlanregistratie(String id) {
    return getPlanregistraties("id = :id", Map.of("id", UUID.fromString(id)), true).stream()
        .findFirst();
  }

  /**
   * @param planregistratieFilter SQL condition for the planregistratie table, only use with constant strings and use
   *     parameters for values
   */
  private List<Planregistratie> getPlanregistraties(
      String planregistratieFilter, Map<String, ?> params, boolean withAantallen) {
    List<Planregistratie> planregistraties = jdbcClient
        .sql("select * from planregistratie where %s order by plan_naam".formatted(planregistratieFilter))
        .params(params)
        .query(planregistratieRowMapper)
        .list();
    if (!withAantallen || planregistraties.isEmpty()) {
      return planregistraties;
    }

    String planregistratieIds = "select id from planregistratie where " + planregistratieFilter;
    Map<UUID, List<DetailplanningRow>> detailplanningen =
        jdbcClient.sql("""
select d.plancategorie_id, d.jaartal, d.aantal_gepland
from detailplanning d
join plancategorie c on c.id = d.plancategorie_id
where c.planregistratie_id in (%s)""".formatted(planregistratieIds)).params(params).query(DetailplanningRow.class).stream()
            .collect(groupingBy(DetailplanningRow::plancategorieId));

    // Ordered by the categorie values in enum order
    Map<UUID, List<PlancategorieRow>> plancategorieen =
        jdbcClient.sql("""
select * from plancategorie
where planregistratie_id in (%s)
order by nieuwbouw, woning_type, wonen_en_zorg, flexwoningen, betaalbaarheid, sloop""".formatted(planregistratieIds)).params(params).query(PlancategorieRow.class).stream()
            .collect(groupingBy(PlancategorieRow::planregistratieId));

    planregistraties.forEach(p -> p.setAantallen(AantallenRows.toAantallen(
        plancategorieen.getOrDefault(UUID.fromString(p.getId()), List.of()), detailplanningen)));
    return planregistraties;
  }

  @Transactional
  public void deletePlanregistratie(String id) {
    jdbcClient
        .sql("delete from planregistratie where id = ?")
        .param(1, id, Types.OTHER)
        .update();
  }

  private record Created(String creator, OffsetDateTime createdAt) {}

  /**
   * Inserts a planregistratie or replaces an existing one, including the aantallen. The creator, createdAt, editor
   * and editedAt fields of the planregistratie are set from the username and the current time, the values from the
   * client are ignored. For an existing planregistratie the creator and createdAt are kept.
   */
  @Transactional
  public void insertPlanregistratie(Planregistratie planregistratie, String username) throws ParseException {
    final String id = planregistratie.getId();
    final OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Amsterdam"));

    Optional<Created> existing = this.jdbcClient
        .sql("select creator, created_at from planregistratie where id = ?")
        .param(1, id, Types.OTHER)
        .query(Created.class)
        .optional();
    planregistratie
        .setCreator(existing.map(Created::creator).orElse(username))
        .setCreatedAt(existing.map(Created::createdAt).orElse(now))
        .setEditor(existing.isPresent() ? username : null)
        .setEditedAt(existing.isPresent() ? now : null);
    this.deletePlanregistratie(id);
    String insertPlanregistratie = """
insert into planregistratie(
id,
geometrie,
creator,
created_at,
editor,
edited_at,
plan_naam,
provincie,
gemeente,
regio,
plaatsnaam,
vertrouwelijkheid,
opdrachtgever_type,
opdrachtgever_naam,
opmerkingen,
plantype,
bestemmingsplan,
status_project,
status_planologisch,
knelpunten_meerkeuze,
beoogd_woonmilieu_abf13,
aantal_studentenwoningen,
sleutelproject
)
values (%s, ?::pmw_knelpunten_meerkeuze[], %s)""".formatted(sqlQuestionMarks(19), sqlQuestionMarks(3));
    this.jdbcClient
        .sql(insertPlanregistratie)
        .param(1, planregistratie.getId(), Types.OTHER)
        .param(wktToWkb(planregistratie.getGeometrie()))
        .param(planregistratie.getCreator())
        .param(planregistratie.getCreatedAt())
        .param(planregistratie.getEditor())
        .param(planregistratie.getEditedAt())
        .param(planregistratie.getPlanNaam())
        .param(planregistratie.getProvincie())
        .param(planregistratie.getGemeente())
        .param(planregistratie.getRegio())
        .param(planregistratie.getPlaatsnaam())
        .param(12, planregistratie.getVertrouwelijkheid(), Types.OTHER)
        .param(13, planregistratie.getOpdrachtgeverType(), Types.OTHER)
        .param(planregistratie.getOpdrachtgeverNaam())
        .param(planregistratie.getOpmerkingen())
        .param(16, planregistratie.getPlantype(), Types.OTHER)
        .param(planregistratie.getBestemmingsplan())
        .param(18, planregistratie.getStatusProject(), Types.OTHER)
        .param(19, planregistratie.getStatusPlanologisch(), Types.OTHER)
        .param(20, planregistratie.getKnelpuntenMeerkeuze(), Types.ARRAY)
        .param(21, planregistratie.getBeoogdWoonmilieuAbf13(), Types.OTHER)
        .param(planregistratie.getAantalStudentenwoningen())
        .param(planregistratie.getSleutelproject())
        .update();

    for (NewPlancategorie p : AantallenRows.toPlancategorieen(planregistratie.getAantallen())) {
      UUID plancategorieId = UUID.randomUUID();
      this.jdbcClient
          .sql("""
insert into plancategorie(id, planregistratie_id, %s, totaal_gepland, totaal_gerealiseerd)
values (?, ?, ?::pmw_%s, ?, ?)""".formatted(p.column(), p.column()))
          .param(1, plancategorieId, Types.OTHER)
          .param(2, id, Types.OTHER)
          .param(3, p.value())
          .param(4, p.aantal().gepland())
          .param(5, p.aantal().gerealiseerd())
          .update();
      if (p.aantal().perJaar() != null) {
        for (Map.Entry<Integer, Integer> jaar : p.aantal().perJaar().entrySet()) {
          this.jdbcClient
              .sql(
                  "insert into detailplanning(plancategorie_id, jaartal, aantal_gepland) values (?, ?, ?)")
              .param(1, plancategorieId, Types.OTHER)
              .param(2, jaar.getKey())
              .param(3, jaar.getValue())
              .update();
        }
      }
    }
  }

  public Optional<String> getProvincieForGemeente(String gemeente) {
    return this.jdbcClient
        .sql("select provincie from gemeente where naam = ?")
        .param(gemeente)
        .query(String.class)
        .optional();
  }

  public String getPlanregistratieGemeente(String id) {
    return (String) this.jdbcClient
        .sql("select gemeente from planregistratie where id = ?")
        .param(1, id, Types.OTHER)
        .query()
        .singleColumn()
        .stream()
        .findFirst()
        .orElse(null);
  }
}
