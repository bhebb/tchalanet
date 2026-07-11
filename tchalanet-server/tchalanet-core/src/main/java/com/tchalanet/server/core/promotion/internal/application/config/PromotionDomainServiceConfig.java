package com.tchalanet.server.core.promotion.internal.application.config;

import com.tchalanet.server.core.promotion.internal.domain.service.PromotionCampaignStateMachine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PromotionDomainServiceConfig {

  @Bean
  PromotionCampaignStateMachine promotionCampaignStateMachine() {
    return new PromotionCampaignStateMachine();
  }
}
