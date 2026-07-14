package com.tchalanet.server.core.sales.internal.application.config;

import com.tchalanet.server.core.sales.api.config.TicketVisibilityProperties;
import com.tchalanet.server.core.sales.internal.domain.service.CustomerTicketStatusResolver;
import com.tchalanet.server.core.sales.internal.domain.service.TicketVisibilityPolicy;
import com.tchalanet.server.core.sales.internal.domain.service.preparation.SalePreparationStateMachine;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SalesDomainServiceConfig {

  @Bean
  CustomerTicketStatusResolver customerTicketStatusResolver() {
    return new CustomerTicketStatusResolver();
  }

  @Bean
  SalePreparationStateMachine salePreparationStateMachine() {
    return new SalePreparationStateMachine();
  }

  @Bean
  TicketVisibilityPolicy ticketVisibilityPolicy(
      TicketVisibilityProperties properties, Clock clock) {
    return new TicketVisibilityPolicy(properties, clock);
  }
}
