/*
 * Copyright (C) 2022 B3Partners B.V.
 *
 * SPDX-License-Identifier: MIT
 */
package org.tailormap.api.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;

/**
 * Internal {@link TailormapUserDetails} implementation.
 *
 * <p>Do not use this class directly; always depend on the {@link TailormapUserDetails} interface instead. This class is
 * {@code public} only to support JSON/session deserialization (e.g. by Jackson / Spring Session). Changing its
 * visibility back to package-private would break that deserialization.
 */
public class TailormapUserDetailsImpl implements TailormapUserDetails {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Collection<GrantedAuthority> authorities;
  private final String username;
  private final String password;
  private final ZonedDateTime validUntil;
  private final boolean enabled;
  private final String organisation;

  private final Collection<TailormapAdditionalProperty> additionalProperties = new ArrayList<>();
  private final Collection<TailormapAdditionalProperty> additionalGroupProperties = new ArrayList<>();

  /**
   * Constructor for Jackson deserialization.
   *
   * @param authorities the authorities
   * @param username the username
   * @param password the password
   * @param validUntil the valid until date
   * @param enabled whether the user is enabled
   * @param organisation the organisation
   * @param additionalProperties the additional properties
   * @param additionalGroupProperties the additional group properties
   */
  @SuppressWarnings("unused")
  TailormapUserDetailsImpl(
      @JsonProperty("authorities") Collection<GrantedAuthority> authorities,
      @JsonProperty("username") String username,
      @JsonProperty("password") String password,
      @JsonProperty("validUntil") ZonedDateTime validUntil,
      @JsonProperty("enabled") boolean enabled,
      @JsonProperty("organisation") String organisation,
      @JsonProperty("additionalProperties") Collection<TailormapAdditionalProperty> additionalProperties,
      @JsonProperty("additionalGroupProperties")
          Collection<TailormapAdditionalProperty> additionalGroupProperties) {
    this.authorities = authorities;
    this.username = username;
    this.password = password;
    this.validUntil = validUntil;
    this.enabled = enabled;
    this.organisation = organisation;
    if (additionalProperties != null) {
      this.additionalProperties.addAll(additionalProperties);
    }
    if (additionalGroupProperties != null) {
      this.additionalGroupProperties.addAll(additionalGroupProperties);
    }
  }

  @Override
  @NonNull public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  @NonNull public String getUsername() {
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
  public String getOrganisation() {
    return organisation;
  }

  @Override
  public Collection<TailormapAdditionalProperty> getAdditionalProperties() {
    return Collections.unmodifiableCollection(additionalProperties);
  }

  @Override
  public Collection<TailormapAdditionalProperty> getAdditionalGroupProperties() {
    return Collections.unmodifiableCollection(additionalGroupProperties);
  }
}
