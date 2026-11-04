/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.validation.EnumValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class EnumValuesConfiguration {
  /** The enum values are read once at startup, after the Flyway migrations, they only change with a migration. */
  @Bean
  public EnumValues enumValues(PlanmonitorWonenDatabaseService pmwDb) {
    return pmwDb.getEnumValues();
  }
}
