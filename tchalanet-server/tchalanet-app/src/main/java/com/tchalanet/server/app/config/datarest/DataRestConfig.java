package com.tchalanet.server.app.config.datarest;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class DataRestConfig implements RepositoryRestConfigurer {

  // Provide a single-arg helper for older/newer compatibility (do not annotate with @Override
  // because some versions only declare the two-arg variant).
  public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
    // Only export repositories that are explicitly annotated with @RepositoryRestResource.
    config.setRepositoryDetectionStrategy(
        RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED);
  }

  @Override
  public void configureRepositoryRestConfiguration(
      RepositoryRestConfiguration config, CorsRegistry cors) {
    configureRepositoryRestConfiguration(config);
  }
}
