/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */
package nl.b3p.planmonitorwonen.api.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = "${planmonitorwonen-api.base-path}/hello",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class HelloController {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @RequestMapping(method = {GET})
  public ResponseEntity<Serializable> hello() {
    logger.debug("Hello, World!");

    record HelloResponse(String message) implements Serializable {}

    return ResponseEntity.status(HttpStatus.OK).body(new HelloResponse("Hello, World!"));
  }
}
