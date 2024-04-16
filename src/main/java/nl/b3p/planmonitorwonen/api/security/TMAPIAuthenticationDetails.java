/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api.security;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthoritiesContainer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class TMAPIAuthenticationDetails extends WebAuthenticationDetails
    implements GrantedAuthoritiesContainer {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final List<String> authorities;

  public TMAPIAuthenticationDetails(HttpServletRequest request) {
    super(request);
    logger.trace("Created TMAPIAuthenticationDetails for request: {}", request.getRequestURI());
    this.authorities = List.of();
  }

  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities() {
    logger.trace("Returning authorities (empty collection) {}", authorities);
    return this.authorities.stream().map(SimpleGrantedAuthority::new).toList();
  }
}
