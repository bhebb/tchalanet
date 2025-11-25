package com.tchalanet.server.tenant.domain.usecase.subscription;

import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.tenant.domain.model.BillingProvider;
import com.tchalanet.server.tenant.domain.model.SubscriptionStatus;
import com.tchalanet.server.tenant.domain.ports.BillingPort;
import com.tchalanet.server.tenant.infra.persistence.JpaPlanRepository;
import com.tchalanet.server.tenant.infra.persistence.JpaSubscriptionRepository;
import com.tchalanet.server.tenant.infra.persistence.PlanJpaEntity;
import com.tchalanet.server.tenant.infra.persistence.SubscriptionJpaEntity;
import com.tchalanet.server.tenant.infra.persistence.SubscriptionMapper;
import com.tchalanet.server.tenant.web.dto.ChangePlanRequest;
import com.tchalanet.server.tenant.web.dto.SubscriptionDTO;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use case pour changer le plan d'abonnement d'un tenant. */
@Service
@RequiredArgsConstructor
public class ChangePlanUseCase {

  private final JpaPlanRepository planRepository;
  private final JpaSubscriptionRepository subscriptionRepository;
  private final BillingPort billingPort;

  @Transactional
  public SubscriptionDTO execute(UUID tenantId, ChangePlanRequest request) {
    // 1. Charger le plan cible (doit être public)
    PlanJpaEntity targetPlan =
        planRepository
            .findById(request.planId())
            .filter(PlanJpaEntity::isPublicPlan)
            .orElseThrow(() -> ProblemRestException.notFound("Plan not found or not public"));

    // 2. Trouver l'abonnement actuel ou en créer un nouveau
    SubscriptionJpaEntity subscription =
        subscriptionRepository
            .findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
                tenantId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING))
            .orElseGet(
                () -> {
                  SubscriptionJpaEntity newSub = new SubscriptionJpaEntity();
                  newSub.setTenantId(tenantId);
                  newSub.setStatus(SubscriptionStatus.ACTIVE);
                  newSub.setPlan(targetPlan);
                  return newSub;
                });

    // 3. Appliquer les règles métier de changement de plan
    subscription.changePlan(targetPlan);

    // 4. Appeler le provider de billing externe
    var billingResult = billingPort.changePlan(tenantId, targetPlan.getCode(), request.proration());
    if (!billingResult.success()) {
      throw ProblemRestException.unprocessable("Billing failed: " + billingResult.message());
    }

    // 5. Mettre à jour les informations de billing
    subscription.setBillingProvider(BillingProvider.NONE);
    subscription.setBillingExternalId(billingResult.externalSubscriptionId().toString());
    subscription.setCurrentPeriodStart(billingResult.periodStart());
    subscription.setCurrentPeriodEnd(billingResult.periodEnd());
    subscription.setMeta(billingResult.meta());

    // 6. Sauvegarder et retourner
    SubscriptionJpaEntity saved = subscriptionRepository.save(subscription);
    return SubscriptionMapper.toDto(saved);
  }

  private UUID safeUuid(String s) {
    if (s == null) return null;
    try {
      return UUID.fromString(s);
    } catch (Exception ex) {
      return null;
    }
  }
}
