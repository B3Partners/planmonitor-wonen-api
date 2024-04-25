/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api.security;

import jakarta.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity(debug = false)
public class ApiSecurityConfig {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public ApiSecurityConfig(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Value("${planmonitorwonen-api.base-path}")
  private String apiBasePath;

  @Bean
  public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        // disable login/logout for this application
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .anonymous(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(
            httpSecuritySessionManagementConfigurer ->
                httpSecuritySessionManagementConfigurer.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            httpSecurityExceptionHandlingConfigurer ->
                httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(
                    (request, response, authException) -> {
                      logger.debug("Unauthorized request: {}", request, authException);
                      response.sendError(
                          HttpServletResponse.SC_UNAUTHORIZED, authException.getLocalizedMessage());
                    }));

    return http.build();
  }

  @Bean(name = "tmapiAuthenticationProvider")
  public AuthenticationProvider tmapiAuthenticationProvider() {
    TMAPIAuthenticationProvider provider = new TMAPIAuthenticationProvider();
    provider.setUserDetailsService(new TMAPIUserDetailsService());
    logger.debug("Created AuthenticationProvider {}", provider);
    return provider;
  }

  @Bean(name = "authenticationManager")
  @DependsOn("tmapiAuthenticationProvider")
  protected AuthenticationManager authenticationManager() {
    return new ProviderManager(Collections.singletonList(tmapiAuthenticationProvider()));
  }

  @Bean
  @DependsOn("authenticationManager")
  public TMAPIAuthenticationFilter tmapiSessionCookieAuthenticationFilter() {
    TMAPIAuthenticationFilter filter = new TMAPIAuthenticationFilter();
    filter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher(apiBasePath + "/**"));
    filter.setAuthenticationManager(authenticationManager());
    filter.setAuthenticationDetailsSource(TMAPIAuthenticationDetails::new);
    filter.setApplicationEventPublisher(applicationEventPublisher);
    logger.debug("TMAPIAuthenticationFilter created with {}", filter);
    return filter;
  }
}
