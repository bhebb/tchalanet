package com.tchalanet.server.core.tenant.domain.usecase.subscription;

import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.core.tenant.domain.model.SubscriptionStatus;
import com.tchalanet.server.core.tenant.infra.persistence.JpaSubscriptionRepository;
import com.tchalanet.server.core.tenant.infra.persistence.SubscriptionMapper;
import com.tchalanet.server.core.tenant.web.dto.SubscriptionDTO;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Use case pour récupérer l'abonnement actuel d'un tenant. */
@Service
@RequiredArgsConstructor
public class GetCurrentSubscriptionUseCase {

  private final JpaSubscriptionRepository subscriptionRepository;

  public SubscriptionDTO execute(UUID tenantId) {
    var subscription =
        subscriptionRepository
            .findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
                tenantId,
                List.of(
                    SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.TRIALING,
                    SubscriptionStatus.CANCELED))
            .orElseThrow(() -> ProblemRestException.notFound("No subscription found for tenant"));

    return SubscriptionMapper.toDto(subscription);
  }
}
