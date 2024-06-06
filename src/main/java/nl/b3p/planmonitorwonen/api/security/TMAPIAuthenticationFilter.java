/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.GenericFilterBean;

/**
 * An authentication filter that checks if the request has a valid session cookie from the TM API.
 */
public class TMAPIAuthenticationFilter extends GenericFilterBean
    implements ApplicationEventPublisherAware {

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ApplicationEventPublisher eventPublisher = null;

  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource =
      new TMASPIAuthenticationDetailsSource();

  private AuthenticationManager authenticationManager = null;

  @Value("${tailormap-api.credentials.url}")
  private String credentialsUrl;

  // TODO: implement cookie path
  //  @Value("${tailormap-api.cookie.path}")
  //  private String cookiePath;

  private RequestMatcher requiresAuthenticationRequestMatcher;
  private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();

  public void setAuthenticationDetailsSource(
      AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
    this.authenticationDetailsSource = authenticationDetailsSource;
  }

  public void setAuthenticationManager(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (this.requiresAuthenticationRequestMatcher.matches((HttpServletRequest) request)) {
      logger.debug(
          "Authenticating {}", this.securityContextHolderStrategy.getContext().getAuthentication());
      doAuthenticate((HttpServletRequest) request, (HttpServletResponse) response);
    } else {
      logger.trace(
          "Did not authenticate since request did not match [{}]",
          this.requiresAuthenticationRequestMatcher);
    }
    chain.doFilter(request, response);
  }

  @Override
  public void afterPropertiesSet() {
    try {
      super.afterPropertiesSet();
    } catch (ServletException ex) {
      throw new RuntimeException(ex);
    }
    Assert.notNull(this.authenticationManager, "An AuthenticationManager must be set");
  }

  private Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    logger.debug("requesting pre-authenticated principal for request: {}", request.getRequestURI());

    Object principal = null;
    final String sessionCookieValue = getSessionCookieValue(request);
    if (null == sessionCookieValue || sessionCookieValue.isBlank()) {
      logger.debug("No session cookie found in request for {}.", request.getRequestURI());
    } else {
      try {
        ObjectNode authResponse = getTMAPIAuthResponse(sessionCookieValue);
        TMAPIAuthenticationToken token = getAuthenticatedPrincipal(authResponse);
        request.setAttribute("authToken", token);
        SecurityContextHolder.getContext().setAuthentication(token);
        logger.debug("Returning principal from token: {}", token);
        principal = token.getPrincipal();
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
    return principal;
  }

  public void setRequiresAuthenticationRequestMatcher(
      RequestMatcher requiresAuthenticationRequestMatcher) {
    Assert.notNull(requiresAuthenticationRequestMatcher, "requestMatcher cannot be null");
    this.requiresAuthenticationRequestMatcher = requiresAuthenticationRequestMatcher;
  }

  private String getSessionCookieValue(HttpServletRequest request) {
    // TODO in theory a request could have more than one JSESSIONID cookies for a url,
    //  so we would have to check all of them, for now just use the first one
    final Cookie jSessionId =
        request.getCookies() == null
            ? null
            : Arrays.stream(request.getCookies())
                .filter(cookie -> "JSESSIONID".equals(cookie.getName()))
                // TODO filter cookie path for the TM API
                //  the current path is / though
                // .filter(cookie -> cookiePath.equals(cookie.getPath()))
                .findFirst()
                .orElse(null);

    if (null == jSessionId) {
      return null;
    }

    logger.debug(
        "Found JSESSIONID = {} in request {}", jSessionId.getValue(), request.getRequestURI());
    return jSessionId.getValue();
  }

  private @NonNull ObjectNode getTMAPIAuthResponse(@NonNull String sessionCookieValue)
      throws JsonProcessingException {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", "JSESSIONID=%s".formatted(sessionCookieValue));
    HttpEntity<ObjectNode> entity = new HttpEntity<>(headers);
    // set up with default TM API unauthorized response
    ObjectNode authResponseBody =
        (ObjectNode) new ObjectMapper().readTree("{\"unauthorized\": true}");

    try {
      // check with TM API if the user is authenticated with the session cookie value
      ResponseEntity<ObjectNode> authResponse =
          restTemplate.exchange(credentialsUrl, HttpMethod.GET, entity, ObjectNode.class);

      if (authResponse.getStatusCode().is2xxSuccessful() && null != authResponse.getBody()) {
        authResponseBody = authResponse.getBody();
        logger.trace("TM API response: {}", authResponseBody);
        if (!authResponse.getBody().get("isAuthenticated").asBoolean()) {
          logger.warn(
              "User {} is not authenticated in TM API", authResponse.getBody().get("username"));
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("User {} is authenticated", authResponse.getBody().get("username"));
            authResponse
                .getBody()
                .get("roles")
                .forEach(role -> logger.debug("Role from TM API: {}", role));
          }
        }
      }

    } catch (HttpClientErrorException e) {
      logger.error("Error while authenticating user: {}", e.getMessage());
    }

    return authResponseBody;
  }

  private @NonNull TMAPIAuthenticationToken getAuthenticatedPrincipal(
      @NonNull ObjectNode authResponse) {
    final Set<GrantedAuthority> authorities = new HashSet<>();
    Objects.requireNonNull(authResponse)
        .get("roles")
        .forEach(
            role -> {
              authorities.add(new SimpleGrantedAuthority(role.asText()));
            });
    return new TMAPIAuthenticationToken(
        authResponse.get("username").asText(), null, authorities, null, authResponse);
  }

  @Override
  public void setApplicationEventPublisher(
      @NonNull ApplicationEventPublisher applicationEventPublisher) {
    this.eventPublisher = applicationEventPublisher;
    try {
      super.afterPropertiesSet();
    } catch (ServletException ex) {
      // convert to RuntimeException for passivity on afterPropertiesSet signature
      throw new RuntimeException(ex);
    }
    Assert.notNull(this.authenticationManager, "An AuthenticationManager must be set");
  }

  private void doAuthenticate(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Object principal = getPreAuthenticatedPrincipal(request);
    if (principal == null) {
      logger.debug("No pre-authenticated principal found in request");
      return;
    }
    logger.debug("pre-authenticated principal = {}, trying to authenticate", principal);
    try {
      TMAPIAuthenticationToken authenticationRequest =
          (TMAPIAuthenticationToken) request.getAttribute("authToken");
      authenticationRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
      Authentication authenticationResult =
          this.authenticationManager.authenticate(authenticationRequest);
      successfulAuthentication(request, response, authenticationResult);
    } catch (AuthenticationException ex) {
      unsuccessfulAuthentication(request, response, ex);
      throw ex;
    }
  }

  protected void successfulAuthentication(
      HttpServletRequest request, HttpServletResponse response, Authentication authResult)
      throws IOException, ServletException {
    logger.debug("Authentication success: {}", authResult);
    SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authResult);
    this.securityContextHolderStrategy.setContext(context);
    this.securityContextRepository.saveContext(context, request, response);

    if (this.eventPublisher != null) {
      this.eventPublisher.publishEvent(
          new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
    }
  }

  protected void unsuccessfulAuthentication(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)
      throws IOException, ServletException {
    this.securityContextHolderStrategy.clearContext();
    logger.debug("Cleared security context due to exception", failed);
    request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, failed);
    // TODO: implement failure handler
    //        if (this.authenticationFailureHandler != null) {
    //          this.authenticationFailureHandler.onAuthenticationFailure(request, response,
    // failed);
    //        }
  }
}
