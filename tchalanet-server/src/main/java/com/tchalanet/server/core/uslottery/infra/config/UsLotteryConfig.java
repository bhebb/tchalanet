package com.tchalanet.server.core.uslottery.infra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(UsLotteryProperties.class)
@ConditionalOnProperty(prefix = "tch.us-lottery", name = "providers", matchIfMissing = true)
public class UsLotteryConfig {

  @Bean
  public WebClient.Builder webClientBuilder() {
    int maxSize = 10 * 1024 * 1024; // 5MB

    var strategies =
        ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(maxSize))
            .build();
    return WebClient.builder().exchangeStrategies(strategies);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "tch.us-lottery.providers.ny",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public WebClient nyLotteryWebClient(WebClient.Builder builder, UsLotteryProperties props) {
    var provider = props.getProviders() != null ? props.getProviders().get("ny") : null;
    String baseUrl = provider != null ? provider.getBaseUrl() : null;
    if (baseUrl == null || baseUrl.isBlank()) {
      return builder.build();
    }
    return builder.baseUrl(baseUrl).build();
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "tch.us-lottery.providers.florida",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public WebClient floridaLotteryWebClient(WebClient.Builder builder, UsLotteryProperties props) {
    var provider = props.getProviders() != null ? props.getProviders().get("florida") : null;
    String baseUrl = provider != null ? provider.getBaseUrl() : null;
    if (baseUrl == null || baseUrl.isBlank()) {
      return builder
          .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
          .defaultHeader("x-partner", "web")
          .defaultHeader(HttpHeaders.ORIGIN, "https://floridalottery.com")
          .defaultHeader(HttpHeaders.REFERER, "https://floridalottery.com/")
          .defaultHeader(HttpHeaders.USER_AGENT, "Tchalanet/1.0 (+https://tchalanet.com)")
          .build();
    }
    return builder
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("x-partner", "web")
        .defaultHeader(HttpHeaders.ORIGIN, "https://floridalottery.com")
        .defaultHeader(HttpHeaders.REFERER, "https://floridalottery.com/")
        .defaultHeader(HttpHeaders.USER_AGENT, "Tchalanet/1.0 (+https://tchalanet.com)")
        .build();
  }
}
