/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package org.tailormap.api.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serial;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class TailormapUserDetails implements UserDetails {
  @Serial private static final long serialVersionUID = 1L;

  public Collection<GrantedAuthority> authorities;
  public String username;
  @JsonIgnore public String password;
  public ZonedDateTime validUntil;
  public boolean enabled;

  @JsonIgnore // Can contain non-public properties
  private final List<Map.Entry<String, Object>> additionalProperties = new ArrayList<>();

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

  public List<Map.Entry<String, Object>> getAdditionalProperties() {
    return additionalProperties;
  }
}
