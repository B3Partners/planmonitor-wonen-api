/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.security;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.tailormap.api.security.TailormapUserDetails;

@Service
public class PlanmonitorAuthenticationService {
  public record PlanmonitorAuthentication(
      Authentication authentication, boolean isProvincie, Set<String> gemeentes) {}

  private final JdbcClient jdbcClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public PlanmonitorAuthenticationService(@Qualifier("tailormapJdbcClient") JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public PlanmonitorAuthentication getFromSecurityContext() throws ResponseStatusException {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new ResponseStatusException(UNAUTHORIZED);
    }

    Map<String, Set<TailormapUserDetails.UDAdditionalProperty>> groupProperties = getGroupProperties();

    // these keys have been registered in the TM API, see:
    // https://github.com/Tailormap/tailormap-api/blob/d4be62bc4d1bf8ed8cdb52f0887590f1fed337f0/src/main/java/org/tailormap/api/persistence/helper/AdminAdditionalPropertyHelper.java#L18-L24
    boolean isProvincie = groupProperties.getOrDefault("typeGebruiker", Collections.emptySet()).stream()
        .map(TailormapUserDetails.UDAdditionalProperty::value)
        .anyMatch("provincie"::equals);

    Set<String> gemeentes = groupProperties.getOrDefault("gemeente", Collections.emptySet()).stream()
        .map(TailormapUserDetails.UDAdditionalProperty::value)
        .map(Object::toString)
        .collect(Collectors.toSet());

    PlanmonitorAuthentication result = new PlanmonitorAuthentication(authentication, isProvincie, gemeentes);

    if (!result.isProvincie && result.gemeentes.isEmpty()) {
      throw new ResponseStatusException(FORBIDDEN);
    }

    return result;
  }

  public Map<String, Set<TailormapUserDetails.UDAdditionalProperty>> getGroupProperties()
      throws AuthenticationException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    Function<Object, TailormapUserDetails.UDAdditionalProperty[]> converter = (Object o) -> {
      try {
        return objectMapper.readValue(
            ((PGobject) o).getValue(), TailormapUserDetails.UDAdditionalProperty[].class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };

    List<TailormapUserDetails.UDAdditionalProperty> properties = jdbcClient
        .sql(
            "select additional_properties from groups where name in (:names) and additional_properties is not null")
        .params(Collections.singletonMap(
            "names",
            authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList()))
        .query()
        .singleColumn()
        .stream()
        .map(converter)
        .flatMap(Stream::of)
        .toList();

    return properties.stream()
        .collect(Collectors.groupingBy(TailormapUserDetails.UDAdditionalProperty::key, Collectors.toSet()));
  }
}
