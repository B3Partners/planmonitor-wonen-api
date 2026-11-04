/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import nl.b3p.planmonitorwonen.api.AantallenRows.DetailplanningRow;
import nl.b3p.planmonitorwonen.api.AantallenRows.NewPlancategorie;
import nl.b3p.planmonitorwonen.api.AantallenRows.PlancategorieRow;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import nl.b3p.planmonitorwonen.api.validation.EnumValues;
import nl.b3p.planmonitorwonen.api.validation.PlanNaamInUseException;
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
import org.springframework.dao.DuplicateKeyException;
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

  /**
   * Deletes a planregistratie including the aantallen.
   *
   * @return the plan name of the deleted planregistratie, for logging, or empty when it did not exist
   */
  @Transactional
  public Optional<String> deletePlanregistratie(String id) {
    return jdbcClient
        .sql("delete from planregistratie where id = ? returning plan_naam")
        .param(UUID.fromString(id))
        .query(String.class)
        .optional();
  }

  private record Created(String creator, OffsetDateTime createdAt) {}

  /**
   * Inserts a planregistratie or replaces an existing one, including the aantallen. The creator, createdAt, editor
   * and editedAt fields of the planregistratie are set from the username and the current time, the values from the
   * client are ignored. For an existing planregistratie the creator and createdAt are kept.
   *
   * @throws PlanNaamInUseException when another planregistratie has the same plan name
   */
  @Transactional
  public void insertPlanregistratie(Planregistratie planregistratie, String username) throws ParseException {
    final UUID id = UUID.fromString(planregistratie.getId());
    final OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Europe/Amsterdam"));

    boolean planNaamInUse = this.jdbcClient
        .sql("select exists(select 1 from planregistratie where plan_naam = ? and id <> ?)")
        .param(planregistratie.getPlanNaam())
        .param(id)
        .query(Boolean.class)
        .single();
    if (planNaamInUse) {
      throw new PlanNaamInUseException(planregistratie.getPlanNaam());
    }

    Optional<Created> existing = this.jdbcClient
        .sql("select creator, created_at from planregistratie where id = ?")
        .param(id)
        .query(Created.class)
        .optional();
    planregistratie
        .setCreator(existing.map(Created::creator).orElse(username))
        .setCreatedAt(existing.map(Created::createdAt).orElse(now))
        .setEditor(existing.isPresent() ? username : null)
        .setEditedAt(existing.isPresent() ? now : null);
    this.deletePlanregistratie(planregistratie.getId());
    try {
      insertPlanregistratieRow(planregistratie);
    } catch (DuplicateKeyException e) {
      // Another planregistratie with the same name was inserted concurrently after the check above
      if (e.getMessage() != null && e.getMessage().contains("planregistratie_plan_naam_key")) {
        throw new PlanNaamInUseException(planregistratie.getPlanNaam());
      }
      throw e;
    }

    for (NewPlancategorie p : AantallenRows.toPlancategorieen(planregistratie.getAantallen())) {
      UUID plancategorieId = UUID.randomUUID();
      this.jdbcClient
          .sql("""
insert into plancategorie(id, planregistratie_id, %s, totaal_gepland, totaal_gerealiseerd)
values (:id, :planregistratieId, :value, :gepland, :gerealiseerd)""".formatted(p.column()))
          .param("id", plancategorieId)
          .param("planregistratieId", id)
          .param("value", p.value(), Types.OTHER)
          .param("gepland", p.aantal().gepland())
          .param("gerealiseerd", p.aantal().gerealiseerd())
          .update();
      if (p.aantal().perJaar() != null) {
        for (Map.Entry<Integer, Integer> jaar : p.aantal().perJaar().entrySet()) {
          this.jdbcClient
              .sql(
                  "insert into detailplanning(plancategorie_id, jaartal, aantal_gepland) values (?, ?, ?)")
              .param(plancategorieId)
              .param(jaar.getKey())
              .param(jaar.getValue())
              .update();
        }
      }
    }
  }

  private void insertPlanregistratieRow(Planregistratie planregistratie) throws ParseException {
    this.jdbcClient
        .sql("""
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
values (
:id,
:geometrie,
:creator,
:createdAt,
:editor,
:editedAt,
:planNaam,
:provincie,
:gemeente,
:regio,
:plaatsnaam,
:vertrouwelijkheid,
:opdrachtgeverType,
:opdrachtgeverNaam,
:opmerkingen,
:plantype,
:bestemmingsplan,
:statusProject,
:statusPlanologisch,
cast(:knelpuntenMeerkeuze as pmw_knelpunten_meerkeuze[]),
:beoogdWoonmilieuAbf13,
:aantalStudentenwoningen,
:sleutelproject
)""")
        .param("id", UUID.fromString(planregistratie.getId()))
        .param("geometrie", wktToWkb(planregistratie.getGeometrie()))
        .param("creator", planregistratie.getCreator())
        .param("createdAt", planregistratie.getCreatedAt())
        .param("editor", planregistratie.getEditor())
        .param("editedAt", planregistratie.getEditedAt())
        .param("planNaam", planregistratie.getPlanNaam())
        .param("provincie", planregistratie.getProvincie())
        .param("gemeente", planregistratie.getGemeente())
        .param("regio", planregistratie.getRegio())
        .param("plaatsnaam", planregistratie.getPlaatsnaam())
        .param("vertrouwelijkheid", planregistratie.getVertrouwelijkheid(), Types.OTHER)
        .param("opdrachtgeverType", planregistratie.getOpdrachtgeverType(), Types.OTHER)
        .param("opdrachtgeverNaam", planregistratie.getOpdrachtgeverNaam())
        .param("opmerkingen", planregistratie.getOpmerkingen())
        .param("plantype", planregistratie.getPlantype(), Types.OTHER)
        .param("bestemmingsplan", planregistratie.getBestemmingsplan())
        .param("statusProject", planregistratie.getStatusProject(), Types.OTHER)
        .param("statusPlanologisch", planregistratie.getStatusPlanologisch(), Types.OTHER)
        .param("knelpuntenMeerkeuze", planregistratie.getKnelpuntenMeerkeuze(), Types.ARRAY)
        .param("beoogdWoonmilieuAbf13", planregistratie.getBeoogdWoonmilieuAbf13(), Types.OTHER)
        .param("aantalStudentenwoningen", planregistratie.getAantalStudentenwoningen())
        .param("sleutelproject", planregistratie.getSleutelproject())
        .update();
  }

  /** Returns the values of the enum types of the planmonitor wonen schema. */
  public EnumValues getEnumValues() {
    record EnumValue(String type, String value) {}
    return new EnumValues(this.jdbcClient.sql("""
select t.typname as type, e.enumlabel as value
from pg_type t
join pg_enum e on e.enumtypid = t.oid
where t.typname like 'pmw\\_%' and pg_type_is_visible(t.oid)
order by t.typname, e.enumsortorder""").query(EnumValue.class).stream()
        .collect(groupingBy(EnumValue::type, mapping(EnumValue::value, toList()))));
  }

  public Optional<String> getProvincieForGemeente(String gemeente) {
    return this.jdbcClient
        .sql("select provincie from gemeente where naam = ?")
        .param(gemeente)
        .query(String.class)
        .optional();
  }

  /** Returns whether the geometry intersects the geometry of the gemeente, or empty for an unknown gemeente. */
  public Optional<Boolean> geometryIntersectsGemeente(String gemeente, String wkt) throws ParseException {
    return this.jdbcClient
        .sql("select ST_Intersects(geometry, ST_GeomFromWKB(?, 28992)) from gemeente where naam = ?")
        .param(wktToWkb(wkt))
        .param(gemeente)
        .query(Boolean.class)
        .optional();
  }

  public String getPlanregistratieGemeente(String id) {
    return (String) this.jdbcClient
        .sql("select gemeente from planregistratie where id = ?")
        .param(UUID.fromString(id))
        .query()
        .singleColumn()
        .stream()
        .findFirst()
        .orElse(null);
  }
}
