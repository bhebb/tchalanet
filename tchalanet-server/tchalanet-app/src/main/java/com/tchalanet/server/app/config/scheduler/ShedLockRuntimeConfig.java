package com.tchalanet.server.app.config.scheduler;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class ShedLockRuntimeConfig {

  @Bean
  public LockProvider lockProvider(@Qualifier("rawDataSource") DataSource dataSource) {
    return new JdbcTemplateLockProvider(dataSource);
  }
}
