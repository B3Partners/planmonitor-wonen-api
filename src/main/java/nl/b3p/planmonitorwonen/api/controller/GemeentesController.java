package nl.b3p.planmonitorwonen.api.controller;

import java.util.List;
import nl.b3p.planmonitorwonen.api.model.Gemeente;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
public class GemeentesController {
  private final JdbcClient jdbcClient;

  public GemeentesController(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @GetMapping(path = "${planmonitor-wonen-api.base-path}/gemeentes")
  public ResponseEntity<List<Gemeente>> planregistraties() {
    List<Gemeente> gemeentes =
        jdbcClient
            .sql(
                "select identificatie, naam, provincie, st_astext(geometry) as geometry from gemeente order by naam")
            .query(Gemeente.class)
            .list();
    return ResponseEntity.ok(gemeentes);
  }
}
