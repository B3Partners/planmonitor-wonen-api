/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import nl.b3p.planmonitorwonen.api.model.Detailplanning;
import nl.b3p.planmonitorwonen.api.model.Plancategorie;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SimplePropertyRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class PlanmonitorWonenDatabaseService {
  private final JdbcClient jdbcClient;
  private final RowMapper<Planregistratie> planregistratieRowMapper;

  public PlanmonitorWonenDatabaseService(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;

    planregistratieRowMapper =
        new SimplePropertyRowMapper<>(Planregistratie.class) {
          @Override
          public Planregistratie mapRow(ResultSet rs, int rowNumber) throws SQLException {
            Planregistratie planregistratie = super.mapRow(rs, rowNumber);
            assert planregistratie != null;
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

  public Set<Planregistratie> getPlanregistratiesForProvincie() {
    return jdbcClient.sql("select * from planregistratie").query(planregistratieRowMapper).set();
  }

  public Set<Planregistratie> getPlanregistratiesForGemeentes(Collection<String> gemeentes) {
    return jdbcClient
        .sql(
            "select * from planregistratie where gemeente in (%s)"
                .formatted(sqlQuestionMarks(gemeentes.size())))
        .params(Arrays.asList(gemeentes.toArray()))
        .query(planregistratieRowMapper)
        .set();
  }

  @Transactional
  public void deletePlanregistratie(String id) {
    jdbcClient.sql("delete from planregistratie where id = ?").param(1, id, Types.OTHER).update();
  }

  @Transactional
  public void insertPlanregistratie(
      Planregistratie planregistratie,
      List<Plancategorie> plancategorieen,
      List<Detailplanning> detailplanningen)
      throws ParseException {
    this.deletePlanregistratie(planregistratie.getId());
    String insertPlanregistratie =
        """
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
values (%s)"""
            .formatted(sqlQuestionMarks(23));
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
        .param(20, planregistratie.getKnelpuntenMeerkeuze(), Types.OTHER)
        .param(21, planregistratie.getBeoogdWoonmilieuAbf13(), Types.OTHER)
        .param(planregistratie.getAantalStudentenwoningen())
        .param(planregistratie.isSleutelproject())
        .update();

    for (Plancategorie p : plancategorieen) {
      this.jdbcClient
          .sql(
              """
            insert into plancategorie(id, planregistratie_id, creator, created_at, editor, edited_at, nieuwbouw, woning_type, wonen_en_zorg, flexwoningen, betaalbaarheid, sloop, totaal_gepland, totaal_gerealiseerd)
            values (%s)"""
                  .formatted(sqlQuestionMarks(14)))
          .param(1, p.id(), Types.OTHER)
          .param(2, p.planregistratieId(), Types.OTHER)
          .param(p.creator())
          .param(p.createdAt())
          .param(p.editor())
          .param(p.editedAt())
          .param(7, p.nieuwbouw(), Types.OTHER)
          .param(8, p.woningType(), Types.OTHER)
          .param(9, p.wonenEnZorg(), Types.OTHER)
          .param(10, p.flexwoningen(), Types.OTHER)
          .param(11, p.betaalbaarheid(), Types.OTHER)
          .param(12, p.sloop(), Types.OTHER)
          .param(p.totaalGepland())
          .param(p.totaalGerealiseerd())
          .update();
    }
    for (Detailplanning d : detailplanningen) {
      this.jdbcClient
          .sql(
              """
            insert into detailplanning(id, plancategorie_id, creator, created_at, editor, edited_at, jaartal, aantal_gepland)
            values (%s)"""
                  .formatted(sqlQuestionMarks(8)))
          .param(1, d.id(), Types.OTHER)
          .param(2, d.plancategorieId(), Types.OTHER)
          .param(d.creator())
          .param(d.createdAt())
          .param(d.editor())
          .param(d.editedAt())
          .param(d.jaartal())
          .param(d.aantalGepland())
          .update();
    }
  }

  public String getPlanregistratieGemeente(String id) {
    return (String)
        this.jdbcClient
            .sql("select gemeente from planregistratie where id = ?")
            .param(1, id, Types.OTHER)
            .query()
            .singleColumn()
            .stream()
            .findFirst()
            .orElse(null);
  }

  public Set<Plancategorie> getPlancategorieen(String planregistratieId) {
    return this.jdbcClient
        .sql("select * from plancategorie where planregistratie_id = ?")
        .param(1, planregistratieId, Types.OTHER)
        .query(Plancategorie.class)
        .set();
  }

  public Set<Plancategorie> getAllPlancategorieen() {
    return this.jdbcClient.sql("select * from plancategorie").query(Plancategorie.class).set();
  }

  public Set<Plancategorie> getAllPlancategorieenForGemeentes(Collection<String> gemeentes) {
    return this.jdbcClient
        .sql(
            "select * from plancategorie where planregistratie_id in (select id from planregistratie where gemeente in (%s))"
                .formatted(sqlQuestionMarks(gemeentes.size())))
        .params(Arrays.asList(gemeentes.toArray()))
        .query(Plancategorie.class)
        .set();
  }

  public Set<Detailplanning> getDetailplanningen(String planregistratieId) {
    return this.jdbcClient
        .sql(
            """
            select * from detailplanning
            where plancategorie_id in (select id from plancategorie where planregistratie_id = ?)""")
        .param(1, planregistratieId, Types.OTHER)
        .query(Detailplanning.class)
        .set();
  }

  public Set<Detailplanning> getAllDetailplanningen() {
    return this.jdbcClient.sql("select * from detailplanning").query(Detailplanning.class).set();
  }

  public Set<Detailplanning> getAllDetailplanningenForGemeentes(Collection<String> gemeentes) {
    return this.jdbcClient
        .sql(
            """
            select * from detailplanning
            where plancategorie_id in
                (select id from plancategorie
                 where planregistratie_id in
                 (select id from planregistratie where gemeente in (%s)))"""
                .formatted(sqlQuestionMarks(gemeentes.size())))
        .params(Arrays.asList(gemeentes.toArray()))
        .query(Detailplanning.class)
        .set();
  }
}
