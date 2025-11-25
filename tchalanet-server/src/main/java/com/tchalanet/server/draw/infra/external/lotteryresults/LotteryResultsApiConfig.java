package com.tchalanet.server.draw.infra.external.lotteryresults;

import com.tchalanet.server.draw.application.port.out.ExternalDrawResultPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class LotteryResultsApiConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "tch.results.lotteryresults",
      name = "enabled",
      havingValue = "true")
  public ExternalDrawResultPort lotteryResultsExternalDrawResultPort(
      WebClient.Builder builder, LotteryResultsApiProperties props) {
    return new HttpLotteryResultsApiAdapter(builder, props);
  }
}
