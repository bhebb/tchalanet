package com.tchalanet.server.platform.identity.internal.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(KeycloakBootstrapProperties.class)
@Configuration
public class KeycloakBootstrapConfig {}
