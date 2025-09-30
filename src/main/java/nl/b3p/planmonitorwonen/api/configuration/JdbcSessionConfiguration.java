/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.configuration;

import java.io.InputStream;
import java.io.ObjectInputStream;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJdbcHttpSession
@Profile("!test")
public class JdbcSessionConfiguration {
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
    GenericConversionService converter = new GenericConversionService();
    converter.addConverter(Object.class, byte[].class, new SerializingConverter());
    converter.addConverter(byte[].class, Object.class, new DeserializingConverter(new CustomDeserializer()));
    return converter;
  }

  static class CustomDeserializer implements Deserializer<Object> {
    @Override
    public Object deserialize(InputStream inputStream) {
      try (ObjectInputStream ois = new ObjectInputStream(inputStream)) {
        return ois.readObject();
      } catch (Exception ignored) {
        return null;
      }
    }
  }
}
