package com.tchalanet.server.core.session.infra.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    SalesSessionProperties.class
})
public class SalesSessionConfig {
}
