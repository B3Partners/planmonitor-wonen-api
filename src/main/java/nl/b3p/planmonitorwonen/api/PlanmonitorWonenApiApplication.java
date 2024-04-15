/*
 * Copyright (C) 2024 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"nl.b3p.planmonitorwonen.api.configuration"})
public class PlanmonitorWonenApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(PlanmonitorWonenApiApplication.class, args);
  }
}
