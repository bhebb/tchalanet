package com.tchalanet.server.app.config.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAdminServer
@ConditionalOnProperty(prefix = "tch.admin.server", name = "enabled", havingValue = "true")
public class SpringBootAdminServerConfig {
}

