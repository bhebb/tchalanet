package com.tchalanet.server.uslottery.infra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(UsLotteryProperties.class)
@ConditionalOnProperty(
    prefix = "tch.us-lottery",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class UsLotteryConfig {

  @Bean
  WebClient nyLotteryWebClient(WebClient.Builder builder, UsLotteryProperties props) {
    return builder.baseUrl(props.getNyBaseUrl()).build();
  }

  @Bean
  WebClient floridaLotteryWebClient(WebClient.Builder builder, UsLotteryProperties props) {
    return builder
        .baseUrl(props.getFloridaBaseUrl())
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("x-partner", "web")
        .defaultHeader(HttpHeaders.ORIGIN, "https://floridalottery.com")
        .defaultHeader(HttpHeaders.REFERER, "https://floridalottery.com/")
        .defaultHeader(HttpHeaders.USER_AGENT, "Tchalanet/1.0 (+https://tchalanet.com)")
        .build();
  }
}
