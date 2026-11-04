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
