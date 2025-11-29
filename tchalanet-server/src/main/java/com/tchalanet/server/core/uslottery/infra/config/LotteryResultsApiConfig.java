package com.tchalanet.server.core.uslottery.infra.config;

import com.tchalanet.server.core.uslottery.domain.ports.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.infra.adapter.lotteryresults.HttpLotteryResultsApiAdapter;
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
  public LatestDrawProviderClient lotteryResultsExternalDrawResultPort( // Changed return type
      WebClient.Builder builder, LotteryResultsApiProperties props) {
    return new HttpLotteryResultsApiAdapter(props);
  }
}
