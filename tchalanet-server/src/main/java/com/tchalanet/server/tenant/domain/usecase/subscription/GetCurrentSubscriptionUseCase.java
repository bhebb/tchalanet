package com.tchalanet.server.tenant.domain.usecase.subscription;

import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.tenant.domain.model.SubscriptionStatus;
import com.tchalanet.server.tenant.infra.persistence.JpaSubscriptionRepository;
import com.tchalanet.server.tenant.infra.persistence.SubscriptionMapper;
import com.tchalanet.server.tenant.web.dto.SubscriptionDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Use case pour récupérer l'abonnement actuel d'un tenant. */
@Service
@RequiredArgsConstructor
public class GetCurrentSubscriptionUseCase {

  private final JpaSubscriptionRepository subscriptionRepository;

  public SubscriptionDTO execute(String tenantId) {
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
