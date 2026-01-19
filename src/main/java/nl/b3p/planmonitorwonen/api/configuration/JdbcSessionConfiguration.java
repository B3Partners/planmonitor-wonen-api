/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableJdbcHttpSession
@Profile("!test")
public class JdbcSessionConfiguration implements BeanClassLoaderAware {
  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${spring.datasource.url}")
  private String dataSourceUrl;

  @Value("${spring.datasource.username}")
  private String dataSourceUsername;

  @Value("${spring.datasource.password}")
  private String dataSourcePassword;

  @Value("${tailormap.datasource.url}")
  private String sessionDataSourceUrl;

  @Value("${tailormap.datasource.username}")
  private String sessionDataSourceUsername;

  @Value("${tailormap.datasource.password}")
  private String sessionDataSourcePassword;

  private static final String CREATE_SESSION_ATTRIBUTE_QUERY =
      """
INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES)
VALUES (?, ?, convert_from(?, 'UTF8')::jsonb)
""";

  private static final String UPDATE_SESSION_ATTRIBUTE_QUERY =
      """
UPDATE %TABLE_NAME%_ATTRIBUTES
SET ATTRIBUTE_BYTES = encode(?, 'escape')::jsonb
WHERE SESSION_PRIMARY_ID = ?
AND ATTRIBUTE_NAME = ?
""";

  private ClassLoader classLoader;

  @Override
  public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Bean
  public DataSource dataSource() {
    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.url(dataSourceUrl);
    dataSourceBuilder.username(dataSourceUsername);
    dataSourceBuilder.password(dataSourcePassword);
    return dataSourceBuilder.build();
  }

  @Bean
  public JdbcClient jdbcClient(DataSource dataSource) {
    return JdbcClient.create(dataSource);
  }

  @Bean(name = "tailormapJdbcClient")
  public JdbcClient tailormapJdbcClient(@Qualifier("tailormapDataSource") DataSource data) {
    return JdbcClient.create(data);
  }

  @Bean
  SessionRepositoryCustomizer<JdbcIndexedSessionRepository> customizer() {
    return (sessionRepository) -> {
      sessionRepository.setCreateSessionAttributeQuery(CREATE_SESSION_ATTRIBUTE_QUERY);
      sessionRepository.setUpdateSessionAttributeQuery(UPDATE_SESSION_ATTRIBUTE_QUERY);
    };
  }

  @Bean(name = {"springSessionDataSource", "tailormapDataSource"})
  @SpringSessionDataSource
  public DataSource sessionDataSource() {
    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.url(sessionDataSourceUrl);
    dataSourceBuilder.username(sessionDataSourceUsername);
    dataSourceBuilder.password(sessionDataSourcePassword);
    return dataSourceBuilder.build();
  }

  @Bean(name = "springSessionTransactionOperations")
  public PlatformTransactionManager springSessionTransactionOperations(
      @Qualifier("springSessionDataSource") DataSource springSessionDatasource) {
    return new DataSourceTransactionManager(springSessionDatasource);
  }

  @Bean("springSessionConversionService")
  public ConversionService springSessionConversionService() {
    BasicPolymorphicTypeValidator.Builder builder = BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("org.tailormap.api.security.")
        .allowIfSubType("org.springframework.security.")
        .allowIfSubType("java.util.")
        .allowIfSubType(java.lang.Number.class)
        .allowIfSubType("java.time.")
        .allowIfBaseType(Object.class);

    JsonMapper mapper = JsonMapper.builder()
        .configure(
            StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION,
            (logger.isDebugEnabled() || logger.isTraceEnabled()))
        .configure(SerializationFeature.INDENT_OUTPUT, (logger.isDebugEnabled() || logger.isTraceEnabled()))
        .addMixIn(
            org.tailormap.api.security.TailormapUserDetailsImpl.class,
            org.tailormap.api.security.TailormapUserDetailsImplMixin.class)
        .addMixIn(
            org.tailormap.api.security.TailormapOidcUser.class,
            org.tailormap.api.security.TailormapOidcUserMixin.class)
        .addModules(SecurityJacksonModules.getModules(this.classLoader, builder))
        .activateDefaultTyping(builder.build(), DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
        .build();

    final GenericConversionService converter = new GenericConversionService();
    // Object -> byte[] (serialize to JSON bytes)
    converter.addConverter(Object.class, byte[].class, source -> {
      try {
        logger.debug("Serializing Spring Session: {}", source);
        return mapper.writerFor(Object.class).writeValueAsBytes(source);
      } catch (JacksonException e) {
        logger.error("Error serializing Spring Session object: {}", source, e);
        throw new ConversionFailedException(
            TypeDescriptor.forObject(source), TypeDescriptor.valueOf(byte[].class), source, e);
      }
    });
    // byte[] -> Object (deserialize from JSON bytes)
    converter.addConverter(byte[].class, Object.class, source -> {
      try {
        logger.debug(
            "Deserializing Spring Session from bytes, length: {} ({})",
            source.length,
            new String(source, StandardCharsets.UTF_8));
        return mapper.readValue(source, Object.class);
      } catch (JacksonException e) {
        String preview;
        try {
          String content = new String(source, StandardCharsets.UTF_8);
          int maxLength = 256;
          if (logger.isDebugEnabled() || logger.isTraceEnabled()) {
            preview = content;
          } else {
            preview = content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
          }
        } catch (Exception ex) {
          preview = "<unavailable>";
        }
        logger.error(
            "Error deserializing Spring Session from bytes, length: {}, preview: {}",
            source.length,
            preview,
            e);
        throw new ConversionFailedException(
            TypeDescriptor.valueOf(byte[].class), TypeDescriptor.valueOf(Object.class), source, e);
      }
    });

    return converter;
  }
}
