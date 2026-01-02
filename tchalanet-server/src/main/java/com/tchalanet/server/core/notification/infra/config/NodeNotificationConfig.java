package com.tchalanet.server.core.notification.infra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NodeNotificationConfigProperties.class)
class NodeNotificationConfig {

  @Bean
  RestClient nodeNotificationClient(
      NodeNotificationConfigProperties props, RestClient.Builder builder) {
    return builder.baseUrl(props.baseUrl()).build();
  }
}
