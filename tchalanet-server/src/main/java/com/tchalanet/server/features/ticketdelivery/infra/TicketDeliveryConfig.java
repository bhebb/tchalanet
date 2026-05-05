package com.tchalanet.server.features.ticketdelivery.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TicketDeliveryProperties.class)
public class TicketDeliveryConfig {

  @Bean("edgeDeliveryRestClient")
  public RestClient edgeDeliveryRestClient(TicketDeliveryProperties props, RestClient.Builder builder) {
    return builder
        .baseUrl(props.edgeBaseUrl())
        .build();
  }
}
