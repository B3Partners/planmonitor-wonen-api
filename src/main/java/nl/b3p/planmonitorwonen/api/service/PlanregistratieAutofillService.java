/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.service;

import static java.util.Comparator.comparingDouble;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.util.PreventLocalEntityResolver;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Determines the provincies, regios and woonmilieus for a geometry using features loaded from WFS services at startup.
 */
@Service
@Profile("!test")
public class PlanregistratieAutofillService implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  @Value("${planmonitor-wonen-api.wfs.ruimte.woonmilieu-typename}")
  private String woonmilieuTypename;

  @Value("${planmonitor-wonen-api.wfs.ruimte.woonmilieu-propertyname}")
  private String woonmilieuPropertyName;

  // Loaded after startup, while requests may already be handled
  private final List<SimpleFeature> provincieFeatures = new CopyOnWriteArrayList<>();
  private final List<SimpleFeature> regioFeatures = new CopyOnWriteArrayList<>();
  private final List<SimpleFeature> woonmilieuFeatures = new CopyOnWriteArrayList<>();

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    Map<String, Object> params = new HashMap<>();
    params.put(WFSDataStoreFactory.TIMEOUT.key, wfsTimeout);
    params.put(WFSDataStoreFactory.ENTITY_RESOLVER.key, PreventLocalEntityResolver.INSTANCE);
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
      logger.info("Loaded ruimte features");
      fs = ds.getFeatureSource(woonmilieuTypename);
      query = new Query(woonmilieuTypename);
      try (SimpleFeatureIterator features = fs.getFeatures(query).features()) {
        while (features.hasNext()) {
          woonmilieuFeatures.add(features.next());
        }
      }
      logger.info("Loaded woonmilieu features");
    } catch (Exception e) {
      logger.error("Error loading features from ruimte WFS " + ruimteWfs, e);
    } finally {
      if (ds != null) {
        ds.dispose();
      }
    }
  }

  private static List<SimpleFeature> getFeaturesOrderedByIntersectionAreaDescending(
      Geometry geometry, Collection<SimpleFeature> features) {
    return features.stream()
        .map(f -> {
          Geometry featureGeometry = (Geometry) f.getDefaultGeometry();
          if (featureGeometry.intersects(geometry)) {
            return Pair.of(f, featureGeometry.intersection(geometry).getArea());
          } else {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .sorted(comparingDouble((Pair<SimpleFeature, Double> p) -> p.getRight())
            .reversed())
        .map(Pair::getLeft)
        .collect(Collectors.toList());
  }

  /** Provincie names intersecting the geometry, ordered by intersection area descending. */
  public List<String> getProvincies(Geometry geometry) {
    return getFeaturesOrderedByIntersectionAreaDescending(geometry, provincieFeatures).stream()
        .map(f -> (String) f.getAttribute(provinciesPropertyName))
        .toList();
  }

  /** Regio names intersecting the geometry, ordered by intersection area descending. */
  public List<String> getRegios(Geometry geometry) {
    return getFeaturesOrderedByIntersectionAreaDescending(geometry, regioFeatures).stream()
        .map(f -> (String) f.getAttribute(regiosPropertyName))
        .toList();
  }

  /** Woonmilieu ABF13 values intersecting the geometry, ordered by intersection area descending. */
  public List<String> getWoonmilieus(Geometry geometry) {
    return getFeaturesOrderedByIntersectionAreaDescending(geometry, woonmilieuFeatures).stream()
        .map(f -> (Integer) f.getAttribute(woonmilieuPropertyName))
        .distinct()
        .map(PlanregistratieAutofillService::woonmilieuCodeToEnumValue)
        .toList();
  }

  private static final Map<Integer, String> woonmilieuCodeValueMap = Map.of(
      2, "Centrum",
      6, "Stedelijk Compact",
      7, "Stedelijk Grondgebonden",
      8, "Stedelijk met Groen",
      9, "Landelijk of Dorps bij de stad",
      10, "Dorps",
      11, "Landelijk");

  private static String woonmilieuCodeToEnumValue(Integer code) {
    return code == null
        ? "Geen Woonmilieu"
        : Objects.requireNonNullElse(woonmilieuCodeValueMap.get(code), "Geen Woonmilieu");
  }
}
