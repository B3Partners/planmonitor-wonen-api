/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Redirects the base path of the API to the Swagger UI, so opening it in a browser shows the API documentation. Not
 * done with an error view for paths that are not found, because the ProblemDetail exception handler answers those
 * before the error page is reached.
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", matchIfMissing = true)
public class SwaggerUiRedirectConfiguration implements WebMvcConfigurer {
  private final String basePath;

  private final String swaggerUiPath;

  public SwaggerUiRedirectConfiguration(
      @Value("${planmonitor-wonen-api.base-path}") String basePath,
      @Value("${springdoc.swagger-ui.path:/swagger-ui.html}") String swaggerUiPath) {
    this.basePath = basePath;
    this.swaggerUiPath = swaggerUiPath;
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController(basePath, swaggerUiPath);
    registry.addRedirectViewController(basePath + "/", swaggerUiPath);
  }
}
