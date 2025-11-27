package com.tchalanet.server.pos.batch;

import com.tchalanet.server.pos.domain.ports.in.AutoCloseSessionsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCloseSessionsJob {

  private final AutoCloseSessionsUseCase autoCloseSessionsUseCase;

  // private final TenantRepositoryPort tenantRepository; // To iterate over tenants

  // This job would typically run periodically, e.g., every 5-10 minutes
  @Scheduled(fixedRateString = "${pos.session.auto-close.fixed-rate:300000}") // 5 minutes
  public void runAutoCloseSessions() {
    log.info("Running scheduled job: AutoCloseSessionsJob");
    // In a multi-tenant system, you would typically iterate over all active tenants
    // and call autoCloseSessions(tenantId) for each.
    // For simplicity, we call it with null to process all, or a specific tenant if configured.
    autoCloseSessionsUseCase.autoCloseSessions(null); // Process all tenants
  }
}
