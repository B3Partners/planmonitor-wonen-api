/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.RequestPath;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Versioning of the API. The major version is a path segment after the base path:
 * {@code /api/planmonitor-wonen/v2/planregistraties}. A new major version means backwards-incompatible changes. The
 * full version (major.minor, the minor version is increased for backwards-compatible additions) is in the
 * {@value #VERSION_HEADER} response header and the OpenAPI documentation.
 *
 * <p>The API version is separate from the version of the application. It starts at 2 because version 1 is the
 * unversioned API used by earlier releases.
 */
@Configuration
public class ApiVersionConfiguration implements WebMvcConfigurer {
  /**
   * Major version as used in the {@code version} attribute of the request mappings. The version parser ignores the
   * "v", but springdoc puts it in the paths of the OpenAPI documentation as is.
   */
  public static final String MAJOR_VERSION = "v2";

  public static final String VERSION = "2.0";

  public static final String VERSION_HEADER = "API-Version";

  private final String basePath;

  private final Pattern versionedPath;

  public ApiVersionConfiguration(@Value("${planmonitor-wonen-api.base-path}") String basePath) {
    this.basePath = basePath;
    this.versionedPath = Pattern.compile("^" + Pattern.quote(basePath) + "/v\\d+(/.*)?$");
  }

  @Override
  public void configureApiVersioning(ApiVersionConfigurer configurer) {
    int versionSegmentIndex = basePath.replaceAll("^/|/$", "").split("/").length;
    configurer.usePathSegment(versionSegmentIndex, this::isVersionedPath);
  }

  /**
   * Only paths with a version segment after the base path are versioned, so Swagger UI and the OpenAPI documentation
   * are left out. The OpenAPI documentation is not at the springdoc default "v3/api-docs" path, which would be taken
   * as API version 3.
   */
  private boolean isVersionedPath(RequestPath path) {
    return versionedPath.matcher(path.pathWithinApplication().value()).matches();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new HandlerInterceptor() {
          @Override
          public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            response.setHeader(VERSION_HEADER, VERSION);
            return true;
          }
        })
        .addPathPatterns(basePath + "/" + MAJOR_VERSION + "/**");
  }

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI().info(new Info().title("Planmonitor Wonen API").version(VERSION));
  }
}
