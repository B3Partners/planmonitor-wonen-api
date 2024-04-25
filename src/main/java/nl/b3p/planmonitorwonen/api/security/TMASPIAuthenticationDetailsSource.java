/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class TMASPIAuthenticationDetailsSource
    implements AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails> {

  @Override
  public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
    return new TMAPIAuthenticationDetails(context);
  }
}
