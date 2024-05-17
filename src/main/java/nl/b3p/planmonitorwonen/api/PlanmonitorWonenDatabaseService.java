/*
 * Copyright (C) 2024 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
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

  public Set<Planregistratie> getPlanregistraties() {
    return jdbcClient.sql("select * from planregistratie").query(planregistratieRowMapper).set();
  }

  public void insertPlanregistratie(Planregistratie planregistratie) throws ParseException {
    String insertPlanregistratie =
            """
insert into planregistratie(
  geometrie,
  creator,
  plan_naam,
  provincie,
  gemeente,
  regio,
  plaatsnaam,
  vertrouwelijkheid,
  opdrachtgever_type,
  opdrachtgever_naam,
  jaar_start_project,
  oplevering_eerste,
  oplevering_laatste,
  opmerkingen,
  plantype,
  bestemmingsplan,
  status_project,
  status_planologisch,
  knelpunten_meerkeuze,
  regionale_planlijst,
  toelichting_knelpunten,
  flexwoningen,
  levensloopbestendig_ja,
  levensloopbestendig_nee,
  beoogd_woonmilieu_abf5,
  beoogd_woonmilieu_abf13,
  aantal_studentenwoningen,
  toelichting_kwalitatief
  )
values (%s)"""
            .formatted(sqlQuestionMarks(28));

    this.jdbcClient
        .sql(insertPlanregistratie)
        .param(1, wktToWkb(planregistratie.getGeometrie()))
        .param(2, planregistratie.getCreator())
        .param(3, planregistratie.getPlanNaam())
        .param(4, planregistratie.getProvincie())
        .param(5, planregistratie.getGemeente())
        .param(6, planregistratie.getRegio())
        .param(7, planregistratie.getPlaatsnaam())
        .param(8, planregistratie.getVertrouwelijkheid(), Types.OTHER)
        .param(9, planregistratie.getOpdrachtgeverType(), Types.OTHER)
        .param(10, planregistratie.getOpdrachtgeverNaam())
        .param(11, planregistratie.getJaarStartProject())
        .param(12, planregistratie.getOpleveringEerste())
        .param(13, planregistratie.getOpleveringLaatste())
        .param(14, planregistratie.getOpmerkingen())
        .param(15, planregistratie.getPlantype(), Types.OTHER)
        .param(16, planregistratie.getBestemmingsplan())
        .param(17, planregistratie.getStatusProject(), Types.OTHER)
        .param(18, planregistratie.getStatusPlanologisch(), Types.OTHER)
        .param(19, planregistratie.getKnelpuntenMeerkeuze(), Types.OTHER)
        .param(20, planregistratie.getRegionalePlanlijst(), Types.OTHER)
        .param(21, planregistratie.getToelichtingKnelpunten(), Types.OTHER)
        .param(22, planregistratie.getFlexwoningen())
        .param(23, planregistratie.getLevensloopbestendigJa())
        .param(24, planregistratie.getLevensloopbestendigNee())
        .param(25, planregistratie.getBeoogdWoonmilieuAbf5(), Types.OTHER)
        .param(26, planregistratie.getBeoogdWoonmilieuAbf13(), Types.OTHER)
        .param(27, planregistratie.getAantalStudentenwoningen())
        .param(28, planregistratie.getToelichtingKwalitatief())
        .update();
  }

  public boolean planregistratieExists(String id) {
    return !this.jdbcClient
        .sql("select 1 from planregistratie where id = ?")
        .param(1, id, Types.OTHER)
        .query()
        .singleColumn()
        .isEmpty();
  }

  public Set<Plancategorie> getPlancategorieen(String planregistratieId) {
    return this.jdbcClient
        .sql("select * from plancategorie where planregistratie_id = ?")
        .param(1, planregistratieId, Types.OTHER)
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
}
