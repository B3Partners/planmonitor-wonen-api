/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import nl.b3p.planmonitorwonen.api.PlanmonitorWonenDatabaseService;
import nl.b3p.planmonitorwonen.api.controller.GemeentesController;
import nl.b3p.planmonitorwonen.api.controller.PlanregistratieAutofillController;
import nl.b3p.planmonitorwonen.api.controller.PlanregistratieController;
import nl.b3p.planmonitorwonen.api.controller.UserInfoController;
import nl.b3p.planmonitorwonen.api.model.Planregistratie;
import nl.b3p.planmonitorwonen.api.security.PlanmonitorAuthenticationService;
import nl.b3p.planmonitorwonen.api.service.PlanregistratieAutofillService;
import nl.b3p.planmonitorwonen.api.service.PlanregistratieCompletionService;
import nl.b3p.planmonitorwonen.api.validation.EnumValues;
import nl.b3p.planmonitorwonen.api.validation.PlanregistratieValidator;
import nl.b3p.planmonitorwonen.api.validation.TestEnumValues;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(
    controllers = {
      PlanregistratieController.class,
      GemeentesController.class,
      PlanregistratieAutofillController.class,
      UserInfoController.class
    },
    properties = {
      "planmonitor-wonen-api.base-path=/api/planmonitor-wonen",
      "springdoc.api-docs.path=/api/planmonitor-wonen/api-docs",
      "springdoc.paths-to-match=/api/planmonitor-wonen/**",
      "springdoc.default-produces-media-type=application/json"
    })
@ImportAutoConfiguration({
  SpringDocConfiguration.class,
  SpringDocConfigProperties.class,
  SpringDocWebMvcConfiguration.class
})
@Import(OpenApiConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration-test")
class OpenApiConfigurationTest {
  private static final String API_DOCS = "/api/planmonitor-wonen/api-docs";
  private static final String PUT_PATH = "$.paths['/api/planmonitor-wonen/v2/planregistratie/{id}'].put";

  @TestConfiguration
  static class EnumValuesConfiguration {
    @Bean
    EnumValues enumValues() {
      return TestEnumValues.ENUM_VALUES;
    }
  }

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private PlanmonitorAuthenticationService authenticationService;

  @MockitoBean
  private PlanmonitorWonenDatabaseService pmwDb;

  @MockitoBean
  private PlanregistratieCompletionService completionService;

  @MockitoBean
  private PlanregistratieAutofillService autofillService;

  @MockitoBean
  private JdbcClient jdbcClient;

  @Test
  void descriptionHasVersionPolicy() {
    assertThat(mvc.get().uri(API_DOCS))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.info.description")
        .asString()
        .contains("/api/planmonitor-wonen/v2/...")
        .contains("nu `2.0`")
        .doesNotContain("{majorVersion}", "{version}");
  }

  @Test
  void enumValuesFromDatabase() {
    assertThat(mvc.get().uri(API_DOCS))
        .bodyJson()
        .extractingPath("$.components.schemas.Planregistratie.properties.plantype.enum")
        .asArray()
        .containsExactlyElementsOf(TestEnumValues.ENUM_VALUES.get(EnumValues.PLANTYPE));
    assertThat(mvc.get().uri(API_DOCS))
        .bodyJson()
        .extractingPath("$.components.schemas.Planregistratie.properties.knelpuntenMeerkeuze.items.enum")
        .asArray()
        .containsExactlyElementsOf(TestEnumValues.ENUM_VALUES.get(EnumValues.KNELPUNTEN_MEERKEUZE));
  }

  @Test
  void categorieGroupsHaveAPropertyPerCategorie() {
    assertThat(mvc.get().uri(API_DOCS))
        .bodyJson()
        .extractingPath("$.components.schemas.Aantallen.properties.woningType.properties")
        .asMap()
        .containsOnlyKeys(TestEnumValues.ENUM_VALUES.get(EnumValues.WONING_TYPE));
  }

  @Test
  void auditFieldsAreReadOnlyAndRequiredFieldsRequired() {
    assertThat(mvc.get().uri(API_DOCS))
        .bodyJson()
        .extractingPath("$.components.schemas.Planregistratie.properties.creator.readOnly")
        .isEqualTo(true);
    assertThat(mvc.get().uri(API_DOCS))
        .bodyJson()
        .extractingPath("$.components.schemas.Planregistratie.required")
        .asArray()
        .containsExactlyInAnyOrder("planNaam", "gemeente", "geometrie", "plantype", "statusPlanologisch");
  }

  @Test
  void putDocumentsCreatedAndErrorResponses() {
    assertThat(mvc.get().uri(API_DOCS))
        .bodyJson()
        .extractingPath(PUT_PATH + ".responses")
        .asMap()
        .containsKeys("200", "201", "400", "401", "403", "409");
    assertThat(mvc.get().uri(API_DOCS))
        .bodyJson()
        .extractingPath(PUT_PATH + ".responses.400.content['application/problem+json'].schema.$ref")
        .isEqualTo("#/components/schemas/ProblemDetail");
  }

  @Test
  void webappHelpersHaveTheirOwnTag() {
    assertThat(mvc.get().uri(API_DOCS))
        .bodyJson()
        .extractingPath("$.paths['/api/planmonitor-wonen/v2/gemeentes'].get.tags")
        .asArray()
        .containsExactly(OpenApiConfiguration.WEBAPP_TAG);
    assertThat(mvc.get().uri(API_DOCS))
        .bodyJson()
        .extractingPath("$.paths['/api/planmonitor-wonen/v2/planregistratie/autofill-by-geometry'].post.tags")
        .asArray()
        .containsExactly(OpenApiConfiguration.WEBAPP_TAG);
  }

  @Test
  void examplePlanregistratieIsValid() throws IOException {
    String json = new ClassPathResource(OpenApiConfiguration.PLANREGISTRATIE_EXAMPLE_RESOURCE)
        .getContentAsString(StandardCharsets.UTF_8);
    Planregistratie planregistratie = JsonMapper.builder().build().readValue(json, Planregistratie.class);
    assertEquals(List.of(), PlanregistratieValidator.validate(planregistratie, TestEnumValues.ENUM_VALUES));
  }
}
