package com.tchalanet.server.tenant.domain.usecase.subscription;

import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.tenant.domain.model.BillingProvider;
import com.tchalanet.server.tenant.domain.model.SubscriptionStatus;
import com.tchalanet.server.tenant.domain.ports.BillingPort;
import com.tchalanet.server.tenant.infra.persistence.JpaSubscriptionRepository;
import com.tchalanet.server.tenant.infra.persistence.SubscriptionMapper;
import com.tchalanet.server.tenant.web.dto.SubscriptionDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use case pour réactiver un abonnement annulé. */
@Service
@RequiredArgsConstructor
public class ResumeSubscriptionUseCase {

  private final JpaSubscriptionRepository subscriptionRepository;
  private final BillingPort billingPort;

  @Transactional
  public SubscriptionDTO execute(String tenantId) {
    // Trouver l'abonnement
    var subscription =
        subscriptionRepository
            .findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
                tenantId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))
            .orElseThrow(() -> ProblemRestException.notFound("No subscription to resume"));

    // Vérifier qu'il est bien en état d'être réactivé
    if (subscription.getStatus() != SubscriptionStatus.CANCELED) {
      throw ProblemRestException.unprocessable("Subscription is not canceled");
    }

    // Appeler le provider de billing
    var billingResult = billingPort.resume(tenantId);
    if (!billingResult.success()) {
      throw ProblemRestException.unprocessable("Billing failed: " + billingResult.message());
    }

    // Réactiver l'abonnement
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setBillingProvider(BillingProvider.NONE);
    subscription.setBillingExternalId(billingResult.externalSubscriptionId());
    subscription.setCurrentPeriodStart(billingResult.periodStart());
    subscription.setCurrentPeriodEnd(billingResult.periodEnd());
    subscription.setMeta(billingResult.meta());

    var saved = subscriptionRepository.save(subscription);
    return SubscriptionMapper.toDto(saved);
  }
}
