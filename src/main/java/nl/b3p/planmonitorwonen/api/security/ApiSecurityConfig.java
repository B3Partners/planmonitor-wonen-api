/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api.security;

import jakarta.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

@Configuration
@EnableWebSecurity(debug = false)
public class ApiSecurityConfig {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${management.endpoints.web.base-path}")
  private String actuatorBasePath;

  @Bean
  public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    RequestCache nullRequestCache = new NullRequestCache();
    /* CSRF protection is disabled on purpose, so API clients only need the SESSION cookie (see
    openapi/description.md). Logging in and out is done by Tailormap, which does check the XSRF-TOKEN. A
    CSRF attack needs a request that the browser sends cross-origin with the SESSION cookie, without a CORS
    preflight. That is not possible here:
    - Tailormap sets the SESSION cookie with SameSite=Lax, so browsers do not send it with cross-site
    POST/PUT/DELETE requests at all.
    - From another origin on the same site SameSite does not apply. But all endpoints that change data use
    PUT or DELETE, which need a CORS preflight, and this application has no CORS configuration so the
    preflight fails. The only POST endpoint (autofill-by-geometry) is read-only, and a page from another
    origin cannot read the response.
    CsrfSafetyTest checks that new POST endpoints or a CORS configuration do not change this.
    */
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authorize -> {
          authorize.requestMatchers(actuatorBasePath + "/health/**").permitAll();
          authorize
              .requestMatchers(actuatorBasePath, actuatorBasePath + "/**")
              .hasAnyAuthority("admin", "actuator");
          // API endpoints check authorization themselves with PlanmonitorAuthenticationService
          authorize.anyRequest().permitAll();
        })
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.NEVER))
        .requestCache((cache) -> cache.requestCache(nullRequestCache))
        .exceptionHandling(httpSecurityExceptionHandlingConfigurer ->
            httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(
                (request, response, authException) -> {
                  logger.debug("Unauthorized request: {}", request, authException);
                  response.sendError(
                      HttpServletResponse.SC_UNAUTHORIZED, authException.getLocalizedMessage());
                }));

    return http.build();
  }
}
