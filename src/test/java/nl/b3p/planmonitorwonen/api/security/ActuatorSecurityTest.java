/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ActuatorSecurityTest {
  private static final String ACTUATOR = "/api/planmonitor-wonen/actuator";

  @Autowired
  private WebApplicationContext context;

  private MockMvcTester mvc;

  /** Set up MockMvc manually with springSecurity() so {@link WithMockUser} is applied to requests. */
  @BeforeEach
  void setUp() {
    mvc = MockMvcTester.create(MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build());
  }

  @Test
  void healthIsPublicWithoutDetails() {
    assertThat(mvc.get().uri(ACTUATOR + "/health"))
        .hasStatusOk()
        .bodyJson()
        .doesNotHavePath("$.components")
        .hasPathSatisfying("$.status", status -> status.assertThat().isEqualTo("UP"));
  }

  @Test
  void infoRequiresAuthentication() {
    assertThat(mvc.get().uri(ACTUATOR + "/info")).hasStatus(HttpStatus.UNAUTHORIZED);
    assertThat(mvc.get().uri(ACTUATOR)).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @WithMockUser(authorities = "planmonitor-wonen")
  void infoForbiddenWithoutAdminOrActuatorGroup() {
    assertThat(mvc.get().uri(ACTUATOR + "/info")).hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  @WithMockUser(authorities = "actuator")
  void infoAllowedForActuatorGroup() {
    assertThat(mvc.get().uri(ACTUATOR + "/info")).hasStatusOk();
  }

  @Test
  @WithMockUser(authorities = "admin")
  void otherEndpointsNotExposed() {
    assertThat(mvc.get().uri(ACTUATOR + "/loggers")).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mvc.get().uri(ACTUATOR + "/prometheus")).hasStatus(HttpStatus.NOT_FOUND);
  }
}
