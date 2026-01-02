package com.tchalanet.server.core.uslottery.infra.config;

import com.tchalanet.server.common.http.RestClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(UsLotteryProperties.class)
@ConditionalOnProperty(prefix = "tch.us-lottery", name = "providers", matchIfMissing = true)
public class UsLotteryConfig {

  @Bean
  public RestClient.Builder restClientBuilder(RestClientFactory factory) {
    return factory.builder();
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "tch.us-lottery.providers.ny",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public RestClient nyLotteryRestClient(RestClient.Builder builder, UsLotteryProperties props) {
    var provider = props.getProviders() != null ? props.getProviders().get("ny") : null;
    String baseUrl = provider != null ? provider.getBaseUrl() : null;
    return (baseUrl == null || baseUrl.isBlank())
        ? builder.build()
        : builder.baseUrl(baseUrl).build();
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "tch.us-lottery.providers.florida",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public RestClient floridaLotteryRestClient(
      RestClient.Builder builder, UsLotteryProperties props) {
    var provider = props.getProviders() != null ? props.getProviders().get("florida") : null;
    String baseUrl = provider != null ? provider.getBaseUrl() : null;

    var b = (baseUrl == null || baseUrl.isBlank()) ? builder : builder.baseUrl(baseUrl);

    return b.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("x-partner", "web")
        .defaultHeader(HttpHeaders.ORIGIN, "https://floridalottery.com")
        .defaultHeader(HttpHeaders.REFERER, "https://floridalottery.com/")
        .defaultHeader(HttpHeaders.USER_AGENT, "Tchalanet/1.0 (+https://tchalanet.com)")
        .build();
  }
}
