package com.tchalanet.server.core.sales.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TicketPublicProperties.class)
public class TicketSalesConfig {}
