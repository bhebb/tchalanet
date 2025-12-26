package com.tchalanet.server.core.notification.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(NodeNotificationConfigProperties.class)
class NodeNotificationConfig {

  @Bean
  WebClient nodeNotificationClient(
      NodeNotificationConfigProperties props, WebClient.Builder builder) {
    return builder.baseUrl(props.baseUrl()).build();
  }
}
