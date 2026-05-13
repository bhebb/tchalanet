package com.tchalanet.server.common.persistence.config;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.persistence.rls.RlsAwareDataSource;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

  private final TchContextResolver tchContextResolver;

  /** RAW datasource (Hikari) bindé directement sur spring.datasource.* */
  @Bean(name = "rawDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.hikari")
  public HikariDataSource rawDataSource() {
    return new HikariDataSource();
  }

  @Bean(name = "dataSource")
  @Primary
  public DataSource jpaDataSource(DataSource rawDataSource) {
    return new RlsAwareDataSource(rawDataSource, tchContextResolver);
  }

  @Bean(name = "batchDataSource") // RAW
  public DataSource batchDataSource(DataSource rawDataSource) {
    return rawDataSource;
  }
}
