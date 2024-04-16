/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api.security;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class TMAPIUserDetailsService
    implements AuthenticationUserDetailsService<TMAPIAuthenticationToken> {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public UserDetails loadUserDetails(TMAPIAuthenticationToken token)
      throws UsernameNotFoundException {
    return this.buildUserDetails(token, token.getAuthorities());
  }

  protected UserDetails buildUserDetails(
      TMAPIAuthenticationToken token, Collection<? extends GrantedAuthority> authorities) {
    logger.debug("building user details for token: {}, authorities: {}", token, authorities);
    return new TMAPIUserDetails(
        token.getName(), authorities.stream().collect(Collectors.toUnmodifiableList()));
  }
}
