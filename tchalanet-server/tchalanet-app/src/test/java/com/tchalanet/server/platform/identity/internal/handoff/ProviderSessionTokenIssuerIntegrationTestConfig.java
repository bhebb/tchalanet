package com.tchalanet.server.platform.identity.internal.handoff;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class ProviderSessionTokenIssuerIntegrationTestConfig {

  @Bean
  @Primary
  ProviderSessionTokenIssuer providerSessionTokenIssuer() {
    return appUserId -> "integration-test-token-" + appUserId;
  }
}
