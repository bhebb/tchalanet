package com.tchalanet.server.core.game.internal.infra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class GameRestConfig implements RepositoryRestConfigurer {

  @Override
  public void configureRepositoryRestConfiguration(
      RepositoryRestConfiguration config, CorsRegistry cors) {
    config
        .getExposureConfiguration()
        .forDomainType(
            com.tchalanet.server.core.game.internal.infra.persistence.GameJpaEntity.class)
        .withCollectionExposure((metadata, httpMethods) -> httpMethods.disable(HttpMethod.DELETE))
        .withItemExposure((metadata, httpMethods) -> httpMethods.disable(HttpMethod.DELETE));
  }
}
