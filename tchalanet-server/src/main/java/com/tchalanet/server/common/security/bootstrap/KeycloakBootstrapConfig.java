package com.tchalanet.server.common.security.bootstrap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(KeycloakBootstrapProperties.class)
@Configuration
public class KeycloakBootstrapConfig {}
