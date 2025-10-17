package com.tchalanet.server.services.subscription;

import com.tchalanet.server.dto.ChangePlanRequest;
import com.tchalanet.server.dto.SubscriptionDTO;
import com.tchalanet.server.error.ProblemRest;
import com.tchalanet.server.mapper.SubscriptionMapper;
import com.tchalanet.server.model.BillingProvider;
import com.tchalanet.server.model.Plan;
import com.tchalanet.server.model.Subscription;
import com.tchalanet.server.model.SubscriptionStatus;
import com.tchalanet.server.port.BillingPort;
import com.tchalanet.server.port.BillingResult;
import com.tchalanet.server.repository.PlanRepository;
import com.tchalanet.server.repository.SubscriptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Profile({"qa", "prod"})
public class SubscriptionService implements ISubscription {

  private final PlanRepository planRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final BillingPort billingPort;

  @Transactional
  public SubscriptionDTO changePlan(String tenantId, ChangePlanRequest req) {
    // 1) charger plan (public)
    Plan target =
        planRepository
            .findById(req.planId())
            .filter(Plan::isPublicPlan)
            .orElseThrow(() -> ProblemRest.notFound("Plan not found or not public"));

    // 2) find current sub (ACTIVE/TRIALING)
    Subscription sub =
        subscriptionRepository
            .findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
                tenantId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING))
            .orElseGet(
                () -> {
                  Subscription s = new Subscription();
                  s.setTenantId(tenantId);
                  s.setStatus(SubscriptionStatus.ACTIVE); // ou TRIALING si tu gères essai
                  s.setPlan(target);
                  return s;
                });

    // 3) règles upgrade/downgrade (ex: upgrade immédiat)
    sub.changePlan(target);

    // 4) appel provider (sync v1)
    BillingResult br = billingPort.changePlan(tenantId, target.getCode(), req.proration());
    if (!br.success()) throw ProblemRest.unprocessable("Billing failed: " + br.message());

    sub.setBillingProvider(BillingProvider.NONE); // NOOP adapter; STRIPE plus tard
    sub.setBillingExternalId(br.externalSubscriptionId());
    sub.setCurrentPeriodStart(br.periodStart());
    sub.setCurrentPeriodEnd(br.periodEnd());
    sub.setMeta(br.meta());

    Subscription saved = subscriptionRepository.save(sub);
    return SubscriptionMapper.toDto(saved);
  }

  @Transactional
  public SubscriptionDTO cancel(String tenantId, boolean atPeriodEnd) {
    Subscription sub =
        subscriptionRepository
            .findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
                tenantId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING))
            .orElseThrow(() -> ProblemRest.notFound("No active subscription"));

    if (atPeriodEnd) {
      sub.scheduleCancelAtPeriodEnd();
      billingPort.cancelAtPeriodEnd(tenantId);
    } else {
      sub.cancelNow();
      billingPort.cancelAtPeriodEnd(tenantId); // ou autre appel provider
    }
    return SubscriptionMapper.toDto(sub);
  }

  @Override
  public SubscriptionDTO resume(String tenantId) {
    return null;
  }

  public SubscriptionDTO currentForTenant(String tenantId) {
    return null;
  }
}
