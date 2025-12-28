package com.tchalanet.server.common.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = "com.tchalanet.server")
@EnableJpaRepositories(basePackages = "com.tchalanet.server")
public class PersistenceConfig {}
