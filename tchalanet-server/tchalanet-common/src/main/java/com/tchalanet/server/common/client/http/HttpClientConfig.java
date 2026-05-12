package com.tchalanet.server.common.client.http;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientConfig {}
