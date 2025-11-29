package com.tchalanet.server.core.tenant.domain.usecase.subscription.impl;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.domain.model.SubscriptionStatus;
import com.tchalanet.server.core.tenant.domain.usecase.subscription.RenewSubscriptionsUseCase;
import com.tchalanet.server.core.tenant.infra.persistence.JpaSubscriptionRepository;
import com.tchalanet.server.core.tenant.infra.persistence.SubscriptionJpaEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RenewSubscriptionsUseCaseImpl implements RenewSubscriptionsUseCase {

  private final JpaSubscriptionRepository jpa;

  @Override
  @Transactional
  public void renewDueSubscriptions() {
    Instant now = Instant.now();
    List<SubscriptionJpaEntity> due =
        jpa.findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus.ACTIVE, now);
    for (SubscriptionJpaEntity e : due) {
      try {
        // simple fake renewal: extend by 30 days
        Instant newEnd = e.getCurrentPeriodEnd().plus(30, ChronoUnit.DAYS);
        e.setCurrentPeriodEnd(newEnd);
        e.setUpdatedAt(Instant.now());
        jpa.save(e);
        log.info(
            "Renewed subscription {} for tenant {} until {}", e.getId(), e.getTenantId(), newEnd);
      } catch (Exception ex) {
        log.warn("Failed to renew subscription {}: {}", e.getId(), ex.getMessage());
      }
    }
  }
}
