package com.tchalanet.server.core.game.infra.config;

import com.tchalanet.server.core.game.infra.persistence.TenantGameJpaEntity;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class TenantGameRestConfig implements RepositoryRestConfigurer {

  @Override
  public void configureRepositoryRestConfiguration(
      RepositoryRestConfiguration config, CorsRegistry cors) {
    config
        .getExposureConfiguration()
        .forDomainType(TenantGameJpaEntity.class)
        .withItemExposure(
            (metadata, httpMethods) -> httpMethods.disable(HttpMethod.POST, HttpMethod.DELETE))
        .withCollectionExposure(
            (metadata, httpMethods) -> httpMethods.disable(HttpMethod.POST, HttpMethod.DELETE));
  }
}
