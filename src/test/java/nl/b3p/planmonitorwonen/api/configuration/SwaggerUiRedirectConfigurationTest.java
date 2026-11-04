/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import nl.b3p.planmonitorwonen.api.controller.UserInfoController;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(
    controllers = UserInfoController.class,
    properties = {
      "planmonitor-wonen-api.base-path=/api/planmonitor-wonen",
      "springdoc.swagger-ui.path=/api/planmonitor-wonen/swagger-ui.html",
      "spring.mvc.problemdetails.enabled=true"
    })
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration-test")
class SwaggerUiRedirectConfigurationTest {
  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private PlanmonitorAuthenticationService authenticationService;

  @ParameterizedTest
  @ValueSource(strings = {"/api/planmonitor-wonen", "/api/planmonitor-wonen/"})
  void basePathRedirectsToSwaggerUi(String path) {
    assertThat(mvc.get().uri(path))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/api/planmonitor-wonen/swagger-ui.html");
  }
}
