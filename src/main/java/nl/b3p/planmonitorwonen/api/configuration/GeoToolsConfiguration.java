/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import org.geotools.util.PreventLocalEntityResolver;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoToolsConfiguration {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @PostConstruct
  public void init() {
    initGeoTools();
  }

  public static void initGeoTools() {
    // GeoTools 35 disallows all entity resolution by default, which also blocks the WFS
    // DescribeFeatureType requests needed to parse features
    GeoTools.init(new Hints(Hints.ENTITY_RESOLVER, PreventLocalEntityResolver.INSTANCE));

    if (logger.isTraceEnabled()) {
      logger.trace("GeoTools initialised: {}", GeoTools.getAboutInfo());
      logger.trace("GeoTools default hints: {}", GeoTools.getDefaultHints());
    }
  }
}
