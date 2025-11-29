package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.core.tenant.domain.model.Subscription;
import com.tchalanet.server.core.tenant.domain.model.SubscriptionStatus;
import com.tchalanet.server.core.tenant.domain.ports.SubscriptionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaSubscriptionRepositoryAdapter implements SubscriptionRepository {

  private final JpaSubscriptionRepository jpa;

  @Override
  public Optional<Subscription> findById(UUID id) {
    return jpa.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Subscription> findFirstActiveByTenant(
      UUID tenantId, Collection<SubscriptionStatus> statuses) {
    return jpa.findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(tenantId, statuses)
        .map(this::toDomain);
  }

  @Override
  public List<Subscription> findByTenantId(UUID tenantId) {
    return jpa.findByTenantId(tenantId).stream().map(this::toDomain).collect(Collectors.toList());
  }

  @Override
  public Subscription save(Subscription subscription) {
    SubscriptionJpaEntity e = subscriptionToEntity(subscription);
    SubscriptionJpaEntity saved = jpa.save(e);
    return toDomain(saved);
  }

  private Subscription toDomain(SubscriptionJpaEntity e) {
    return new Subscription(
        e.getId(),
        e.getTenantId(),
        e.getPlan() == null ? null : e.getPlan().getId(),
        e.getStatus(),
        e.getCurrentPeriodStart(),
        e.getCurrentPeriodEnd(),
        e.isCancelAtPeriodEnd(),
        e.getBillingProvider(),
        e.getBillingExternalId(),
        e.getMeta(),
        e.getVersion());
  }

  private SubscriptionJpaEntity subscriptionToEntity(Subscription s) {
    SubscriptionJpaEntity e = new SubscriptionJpaEntity();
    e.setId(s.id());
    e.setTenantId(s.tenantId());
    e.setCurrentPeriodStart(s.currentPeriodStart());
    e.setCurrentPeriodEnd(s.currentPeriodEnd());
    e.setStatus(s.status());
    e.setCancelAtPeriodEnd(s.cancelAtPeriodEnd());
    e.setBillingProvider(s.billingProvider());
    e.setBillingExternalId(s.billingExternalId());
    e.setMeta(s.meta());
    e.setVersion(s.version());
    return e;
  }
}
