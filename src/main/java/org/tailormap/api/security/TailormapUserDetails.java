/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package org.tailormap.api.security;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class TailormapUserDetails implements UserDetails {
  public record UDAdditionalProperty(String key, Boolean isPublic, Object value)
      implements Serializable {}

  @Serial private static final long serialVersionUID = 2L;

  public Collection<GrantedAuthority> authorities;
  public String username;
  public String password;
  public ZonedDateTime validUntil;
  public boolean enabled;

  private final List<UDAdditionalProperty> additionalProperties = new ArrayList<>();
  private final List<UDAdditionalProperty> additionalGroupProperties = new ArrayList<>();

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

  public List<UDAdditionalProperty> getAdditionalProperties() {
    return additionalProperties;
  }

  public List<UDAdditionalProperty> getAdditionalGroupProperties() {
    return additionalGroupProperties;
  }
}
