/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.Set;
import java.util.stream.Collectors;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.controller.GemeentesController;
import nl.b3p.planmonitorwonen.api.controller.PlanregistratieAutofillController;
import nl.b3p.planmonitorwonen.api.controller.PlanregistratieController;
import nl.b3p.planmonitorwonen.api.controller.UserInfoController;
import nl.b3p.planmonitorwonen.api.service.PlanregistratieAutofillService;
import nl.b3p.planmonitorwonen.api.service.PlanregistratieCompletionService;
import nl.b3p.planmonitorwonen.api.validation.EnumValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * CSRF protection is disabled in {@link ApiSecurityConfig}. These tests check the assumptions that make that safe, so
 * that a change that breaks them fails here instead of silently allowing CSRF.
 */
@WebMvcTest(
    controllers = {
      GemeentesController.class,
      PlanregistratieAutofillController.class,
      PlanregistratieController.class,
      UserInfoController.class
    })
@Import(ApiSecurityConfig.class)
@ActiveProfiles("integration-test")
class CsrfSafetyTest {
  /**
   * POST endpoints that do not change any data. A cross-origin page can send a POST without a CORS preflight (with a
   * form or text/plain content type), so an endpoint that changes data must use PUT, PATCH or DELETE instead, or CSRF
   * protection must be enabled.
   */
  private static final Set<String> READ_ONLY_POST_PATTERNS =
      Set.of("/{version}/planregistratie/autofill-by-geometry");

  @Value("${planmonitor-wonen-api.base-path}")
  private String basePath;

  @Autowired
  private WebApplicationContext context;

  @MockitoBean
  private PlanmonitorAuthenticationService authenticationService;

  @MockitoBean
  private PlanmonitorWonenDatabaseService planmonitorWonenDatabaseService;

  @MockitoBean
  private PlanregistratieCompletionService completionService;

  @MockitoBean
  private PlanregistratieAutofillService autofillService;

  @MockitoBean
  private JdbcClient jdbcClient;

  @MockitoBean
  private EnumValues enumValues;

  @Autowired
  @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping handlerMapping;

  private MockMvcTester mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcTester.create(MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build());
  }

  @Test
  void controllersAreMapped() {
    assertThat(handlerMapping.getHandlerMethods().values())
        .extracting(handlerMethod -> handlerMethod.getBeanType().getSimpleName())
        .contains("PlanregistratieController", "PlanregistratieAutofillController");
  }

  @Test
  void onlyReadOnlyPostEndpoints() {
    Set<String> postPatterns = handlerMapping.getHandlerMethods().entrySet().stream()
        .filter(entry -> entry.getValue().getBeanType().getPackageName().startsWith("nl.b3p.planmonitorwonen"))
        .filter(entry -> {
          Set<RequestMethod> methods =
              entry.getKey().getMethodsCondition().getMethods();
          // A mapping without methods accepts all methods, including POST
          return methods.isEmpty() || methods.contains(RequestMethod.POST);
        })
        .flatMap(entry -> entry.getKey().getPatternValues().stream())
        .map(pattern -> pattern.substring(basePath.length()))
        .collect(Collectors.toSet());

    assertThat(postPatterns)
        .as("POST endpoints must be read-only, see ApiSecurityConfig")
        .isEqualTo(READ_ONLY_POST_PATTERNS);
  }

  /**
   * Without a CORS configuration Spring answers a preflight with 200 but without Access-Control-Allow-* headers, so
   * the browser does not send the actual cross-origin request.
   */
  @Test
  void crossOriginPreflightIsNotAllowed() {
    assertThat(mvc.options()
            .uri(basePath + "/v2/planregistratie/0b5c1d0e-3f43-4e4a-9a6e-2f1f6b6d2a10")
            .header(HttpHeaders.ORIGIN, "https://attacker.example.com")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
        .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
        .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
        .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
  }
}
