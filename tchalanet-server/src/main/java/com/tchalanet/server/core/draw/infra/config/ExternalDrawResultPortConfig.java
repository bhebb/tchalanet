package com.tchalanet.server.core.draw.infra.config;

import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.core.uslottery.domain.ports.out.LatestDrawProviderClient;
import com.tchalanet.server.core.uslottery.infra.adapter.UsLotteryExternalDrawResultPortAdapter;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides an ExternalDrawResultPort bean by delegating to available latest-draw providers.
 * Uses ConditionalOnMissingBean to allow overriding with other implementations.
 */
@Configuration
public class ExternalDrawResultPortConfig {

  @Bean
  @ConditionalOnMissingBean(ExternalDrawResultPort.class)
  public ExternalDrawResultPort externalDrawResultPort(List<LatestDrawProviderClient> providers) {
    return new UsLotteryExternalDrawResultPortAdapter(providers);
  }
}
