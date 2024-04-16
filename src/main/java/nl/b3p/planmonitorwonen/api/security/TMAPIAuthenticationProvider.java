/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api.security;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class TMAPIAuthenticationProvider
    implements AuthenticationProvider, InitializingBean, Ordered {

  private int order = -1; // default: same as non-ordered

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private TMAPIUserDetailsService tmapiUserDetailsService;

  @Override
  public void afterPropertiesSet() {
    // Assert.notNull(this.tmapiUserDetailsService, "A TMAPIUserDetailsService must be set");
  }

  public void setUserDetailsService(TMAPIUserDetailsService tmapiUserDetailsService) {
    this.tmapiUserDetailsService = tmapiUserDetailsService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    logger.debug("Authenticating with TMAPIAuthenticationProvider for {}", authentication);

    UserDetails details =
        tmapiUserDetailsService.loadUserDetails((TMAPIAuthenticationToken) authentication);

    logger.trace("UserDetails: {}", details);
    logger.trace("Authentication: {}", authentication);

    return new TMAPIAuthenticationToken(
        details.getUsername(),
        details.getPassword(),
        details.getAuthorities(),
        details,
        ((TMAPIAuthenticationToken) authentication).getAuthResponse());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    final boolean supports = TMAPIAuthenticationToken.class.isAssignableFrom(authentication);
    logger.trace(
        "TMAPIAuthenticationProvider {} support {}",
        supports ? "DOES" : "DOES NOT",
        authentication);
    return supports;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  public void setOrder(int i) {
    this.order = i;
  }
}
