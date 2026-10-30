/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

/**
 * Redirects browsers to the Swagger UI instead of showing the Whitelabel Error Page for paths that are not found. Only
 * used for requests accepting HTML, API clients still get a JSON error response.
 */
@Component
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", matchIfMissing = true)
public class SwaggerUiErrorViewResolver implements ErrorViewResolver {
  private final SwaggerUiConfigProperties swaggerUiConfigProperties;

  public SwaggerUiErrorViewResolver(SwaggerUiConfigProperties swaggerUiConfigProperties) {
    this.swaggerUiConfigProperties = swaggerUiConfigProperties;
  }

  @Override
  public ModelAndView resolveErrorView(
      @NonNull HttpServletRequest request, @NonNull HttpStatus status, @NonNull Map<String, Object> model) {
    if (status == HttpStatus.NOT_FOUND) {
      return new ModelAndView("redirect:" + swaggerUiConfigProperties.getPath());
    }
    return null;
  }
}
