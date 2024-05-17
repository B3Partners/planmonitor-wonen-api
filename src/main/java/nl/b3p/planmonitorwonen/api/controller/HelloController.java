/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import nl.b3p.planmonitorwonen.api.security.TMAPIAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = "${planmonitor-wonen-api.base-path}/hello",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class HelloController {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private record HelloResponse(String message, ObjectNode authResponse) implements Serializable {}

  @RequestMapping(
      method = {GET},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Serializable> hello(@AuthenticationPrincipal UserDetails userDetails) {
    final TMAPIAuthenticationToken authentication =
        (TMAPIAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

    logger.info("Authentication: {}", authentication);
    logger.info("UserDetails: {}", userDetails);

    if (null == authentication) {
      // should not happen
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new HelloResponse("Unauthorized", null));
    }

    return ResponseEntity.status(HttpStatus.OK)
        .body(
            new HelloResponse(
                "Hello "
                    + authentication.getName()
                    + " with roles: "
                    + authentication.getAuthorities()
                    + " and userDetails: "
                    + userDetails,
                authentication.getAuthResponse()));
  }
}
