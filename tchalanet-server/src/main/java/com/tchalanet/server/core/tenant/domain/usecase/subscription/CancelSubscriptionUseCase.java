package com.tchalanet.server.core.tenant.domain.usecase.subscription;

import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.core.tenant.domain.model.SubscriptionStatus;
import com.tchalanet.server.core.tenant.domain.ports.BillingPort;
import com.tchalanet.server.core.tenant.infra.persistence.JpaSubscriptionRepository;
import com.tchalanet.server.core.tenant.infra.persistence.SubscriptionMapper;
import com.tchalanet.server.core.tenant.web.dto.SubscriptionDTO;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case pour annuler un abonnement. Peut annuler immédiatement ou à la fin de la période en
 * cours.
 */
@Service
@RequiredArgsConstructor
public class CancelSubscriptionUseCase {

  private final JpaSubscriptionRepository subscriptionRepository;
  private final BillingPort billingPort;

  @Transactional
  public SubscriptionDTO execute(UUID tenantId, boolean atPeriodEnd) {
    // Trouver l'abonnement actif
    var subscription =
        subscriptionRepository
            .findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
                tenantId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING))
            .orElseThrow(() -> ProblemRestException.notFound("No active subscription"));

    if (atPeriodEnd) {
      // Annulation différée à la fin de la période
      subscription.scheduleCancelAtPeriodEnd();
      billingPort.cancelAtPeriodEnd(tenantId);
    } else {
      // Annulation immédiate
      subscription.cancelNow();
      billingPort.cancelAtPeriodEnd(tenantId); // Notifier le provider
    }

    var saved = subscriptionRepository.save(subscription);
    return SubscriptionMapper.toDto(saved);
  }
}
