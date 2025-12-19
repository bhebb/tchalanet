package com.tchalanet.server.core.draw.infra.config;

import org.springframework.context.annotation.Configuration;

/**
 * Provides an ExternalDrawResultPort bean by delegating to available latest-draw providers.
 * Uses ConditionalOnMissingBean to allow overriding with other implementations.
 */
@Configuration
public class ExternalDrawResultPortConfig {

  // DUPLICATE DISABLED:
  // ExternalDrawResultPort is provided by core.uslottery.infra.adapter.UsLotteryExternalDrawResultPortAdapter
  // (@Component, @Primary). Keeping this config class only for historical reference.
  // (No @Bean to avoid package drift and duplicate wiring.)
}
