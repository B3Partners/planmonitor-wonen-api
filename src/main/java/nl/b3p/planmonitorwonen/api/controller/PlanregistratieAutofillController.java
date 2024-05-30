package nl.b3p.planmonitorwonen.api.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("!test")
public class PlanregistratieAutofillController
    implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final JdbcClient jdbcClient;

  @Value("${planmonitor-wonen-api.wfs.bestuurlijke-gebieden}")
  private String bestuurlijkeGebiedenWfs;

  @Value("${planmonitor-wonen-api.wfs.bestuurlijke-gebieden.provincies-typename}")
  private String provinciesTypename;

  @Value("${planmonitor-wonen-api.wfs.bestuurlijke-gebieden.provincies-propertyname}")
  private String provinciesPropertyName;

  @Value("${planmonitor-wonen-api.wfs.timeout:5000}")
  private int wfsTimeout;

  @Value("${planmonitor-wonen-api.wfs.ruimte}")
  private String ruimteWfs;

  @Value("${planmonitor-wonen-api.wfs.ruimte.regios-typename}")
  private String regiosTypename;

  @Value("${planmonitor-wonen-api.wfs.ruimte.regios-propertyname}")
  private String regiosPropertyName;

  /*
    @Value("${planmonitor-wonen-api.wfs.ruimte.woonmilieu-typename}")
    private String woonmilieuTypename;
  */

  private List<SimpleFeature> provincieFeatures = new ArrayList<>();
  private List<SimpleFeature> regioFeatures = new ArrayList<>();

  //  private List<SimpleFeature> woonmilieuFeatures = new ArrayList<>();

  public PlanregistratieAutofillController(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    Map<String, Object> params = new HashMap<>();
    params.put(WFSDataStoreFactory.TIMEOUT.key, wfsTimeout);
    DataStore ds = null;

    try {
      params.put(WFSDataStoreFactory.URL.key, bestuurlijkeGebiedenWfs);
      ds = DataStoreFinder.getDataStore(params);
      logger.info("Initialized datastore for bestuurlijke gebieden WFS " + bestuurlijkeGebiedenWfs);

      SimpleFeatureSource fs = ds.getFeatureSource(provinciesTypename);
      Query query = new Query(provinciesTypename);
      try (SimpleFeatureIterator features = fs.getFeatures(query).features()) {
        while (features.hasNext()) {
          provincieFeatures.add(features.next());
        }
      }
      logger.info("Loaded provincie features");
    } catch (Exception e) {
      logger.error("Error loading provincies from WFS " + bestuurlijkeGebiedenWfs, e);
    } finally {
      if (ds != null) {
        ds.dispose();
      }
    }

    try {
      params.put(WFSDataStoreFactory.URL.key, ruimteWfs);
      ds = DataStoreFinder.getDataStore(params);
      logger.info("Initialized datastore for ruimte WFS " + ruimteWfs);

      SimpleFeatureSource fs = ds.getFeatureSource(regiosTypename);
      Query query = new Query(regiosTypename);
      try (SimpleFeatureIterator features = fs.getFeatures(query).features()) {
        while (features.hasNext()) {
          regioFeatures.add(features.next());
        }
      }
      /*
            fs = ds.getFeatureSource(woonmilieuTypename);
            query = new Query(woonmilieuTypename);
            try (SimpleFeatureIterator features = fs.getFeatures(query).features()) {
              while (features.hasNext()) {
                woonmilieuFeatures.add(features.next());
              }
            }
      */
      logger.info("Loaded ruimte features");
    } catch (Exception e) {
      logger.error("Error loading features from ruimte WFS " + ruimteWfs, e);
    } finally {
      if (ds != null) {
        ds.dispose();
      }
    }
  }

  private static List<SimpleFeature> selectFeaturesOrderedByIntersectionArea(
      Geometry geometry, Collection<SimpleFeature> features) {
    return features.stream()
        .map(
            f -> {
              Geometry featureGeometry = (Geometry) f.getDefaultGeometry();
              if (featureGeometry.intersects(geometry)) {
                return Pair.of(f, featureGeometry.intersection(geometry).getArea());
              } else {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparingDouble(Pair::getRight))
        .map(Pair::getLeft)
        .collect(Collectors.toList());
  }

  // See https://www.w3.org/TR/sdw-bp/#applicability-formatVbp, use text/plain content type for WKT
  // in request body
  @PostMapping(
      path = "${planmonitor-wonen-api.base-path}/planregistratie/autofill-by-geometry",
      consumes = "text/plain")
  public Map<String, Object> autofill(@RequestBody String wkt) {
    Geometry geometry;
    try {
      GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 28992);
      geometry = new WKTReader(geometryFactory).read(wkt);
    } catch (ParseException e) {
      throw new ResponseStatusException(BAD_REQUEST, "WKT parsing error");
    }

    List<String> gemeentes =
        this.jdbcClient
            .sql(
                """
        select g.naam
        from gemeente g
        where st_intersects(g.geometry, st_geomfromtext(?, 28992))
        order by st_area(st_intersection(g.geometry, st_geomfromtext(?, 28992))) desc""")
            .param(wkt)
            .param(wkt)
            .query(String.class)
            .list();

    List<String> provincies =
        selectFeaturesOrderedByIntersectionArea(geometry, provincieFeatures).stream()
            .map(f -> (String) f.getAttribute(provinciesPropertyName))
            .toList();

    List<String> regios =
        selectFeaturesOrderedByIntersectionArea(geometry, regioFeatures).stream()
            .map(f -> (String) f.getAttribute(regiosPropertyName))
            .toList();

    return Map.of(
        "gemeentes", gemeentes,
        "provincies", provincies,
        "regios", regios,
        "woonmilieus", new String[] {});
  }
}
