/*
 * Copyright (C) 2022 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */
package org.tailormap.api.security;

import java.io.Serial;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

class TailormapUserDetailsImpl implements TailormapUserDetails {

  @Serial
  private static final long serialVersionUID = 1L;

  private Collection<GrantedAuthority> authorities;
  private String username;
  private String password;
  private ZonedDateTime validUntil;
  private boolean enabled;

  private final Collection<TailormapAdditionalProperty> additionalProperties = new ArrayList<>();
  private final Collection<TailormapAdditionalProperty> additionalGroupProperties = new ArrayList<>();

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return validUntil == null || validUntil.isAfter(ZonedDateTime.now(ZoneId.systemDefault()));
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public Collection<TailormapAdditionalProperty> getAdditionalProperties() {
    return additionalProperties;
  }

  @Override
  public Collection<TailormapAdditionalProperty> getAdditionalGroupProperties() {
    return additionalGroupProperties;
  }
}
