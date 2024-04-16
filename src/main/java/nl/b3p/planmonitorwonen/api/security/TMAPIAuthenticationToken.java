/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;

public class TMAPIAuthenticationToken extends AbstractAuthenticationToken
    implements Authentication, CredentialsContainer {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Object principal;
  private final Object credentials;
  private ObjectNode authResponse;

  /**
   * Creates an unauthenticated token.
   *
   * @param principal the principal to be associated with this authentication object (cannot be
   *     {@code null})
   * @param credentials the credentials to be associated with this authentication object
   */
  public TMAPIAuthenticationToken(Object principal, Object credentials) {
    super(null);
    this.principal = principal;
    this.credentials = credentials;
    setAuthenticated(false);
  }

  /**
   * Creates an authenticated token with the supplied principal, credentials and array of
   * authorities.
   *
   * @param principal the principal to be associated with this authentication object (cannot be
   *     {@code null})
   * @param credentials the credentials to be associated with this authentication object
   * @param authorities the collection of {@code GrantedAuthority}s for the principal
   * @param details additional details for the authentication request
   * @param authResponse the authentication response from the TM API
   */
  public TMAPIAuthenticationToken(
      Object principal,
      Object credentials,
      Collection<? extends GrantedAuthority> authorities,
      Object details,
      ObjectNode authResponse) {
    super(authorities);
    this.principal = principal;
    this.credentials = credentials;
    setDetails(details);
    this.authResponse = authResponse;
    super.setAuthenticated(true); // must use super, as we override
  }

  @Override
  public Object getCredentials() {
    logger.trace("credentials from token {}", this.credentials);
    return this.credentials;
  }

  @Override
  public Object getPrincipal() {
    logger.trace("principal from token {}", this.principal);
    return this.principal;
  }

  public ObjectNode getAuthResponse() {
    return authResponse;
  }

  public void setAuthResponse(ObjectNode authResponse) {
    this.authResponse = authResponse;
  }

  @Override
  public boolean isAuthenticated() {
    if (null != this.authResponse && this.authResponse.has("isAuthenticated")) {
      logger.debug(
          "isAuthenticated from token {}", this.authResponse.get("isAuthenticated").asBoolean());
      return this.authResponse.get("isAuthenticated").asBoolean();
    } else {
      return false;
    }
  }
}
