package com.tchalanet.server.core.config;

import com.tchalanet.server.core.haiti.internal.domain.lottery.service.DefaultHaitiResultProjector;
import com.tchalanet.server.core.haiti.internal.domain.lottery.service.HaitiResultProjector;
import com.tchalanet.server.core.pagemodel.internal.domain.policy.PublishPolicy;
import com.tchalanet.server.core.sales.internal.domain.service.BetSelectionNormalizer;
import com.tchalanet.server.core.sales.internal.domain.service.TicketWinningCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoreDomainServicesConfig {

  @Bean
  HaitiResultProjector haitiResultProjector() {
    return new DefaultHaitiResultProjector();
  }

  @Bean
  PublishPolicy publishPolicy() {
    return new PublishPolicy();
  }

  @Bean
  BetSelectionNormalizer betSelectionNormalizer() {
    return new BetSelectionNormalizer();
  }

  @Bean
  TicketWinningCalculator ticketWinningCalculator() {
    return new TicketWinningCalculator();
  }
}
