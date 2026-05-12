package com.tchalanet.server.core.subscription.internal.infra.persistence;

import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.subscription.application.port.out.SubscriptionPersistencePort;
import com.tchalanet.server.core.subscription.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.core.subscription.domain.model.Subscription;
import com.tchalanet.server.core.subscription.domain.model.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter implementing subscription persistence ports.
 * Maps domain Subscription <-> JPA SubscriptionJpaEntity.
 * Per REFACTORING_GUIDE.md: planId → planCode (string), no billingProvider fields.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter
    implements SubscriptionPersistencePort, SubscriptionReaderPort {

  private final SubscriptionJpaRepository repository;

  @Override
  public Subscription save(Subscription subscription) {
    var entity = repository.findByTenantIdAndDeletedAtIsNull(subscription.tenantId().value())
        .orElse(new SubscriptionJpaEntity());

    entity.setTenantId(subscription.tenantId().value());
    entity.setPlanCode(subscription.planCode()); // ✅ string soft reference
    entity.setStatus(subscription.status().name());
    entity.setStartedAt(subscription.startedAt());
    entity.setEndsAt(subscription.endsAt());
    entity.setTrialEndsAt(subscription.trialEndsAt());
    entity.setCanceledAt(subscription.canceledAt());
    entity.setMetadataJson(subscription.metadata());

    var saved = repository.save(entity);
    return toDomain(saved);
  }

  @Override
  public Optional<Subscription> findByTenantId(TenantId tenantId) {
    return repository.findByTenantIdAndDeletedAtIsNull(tenantId.value())
        .map(this::toDomain);
  }

  private Subscription toDomain(SubscriptionJpaEntity entity) {
    return new Subscription(
        SubscriptionId.of(entity.getId()),
        TenantId.of(entity.getTenantId()),
        entity.getPlanCode(), // ✅ string
        SubscriptionStatus.valueOf(entity.getStatus()),
        entity.getStartedAt(),
        entity.getEndsAt(),
        entity.getTrialEndsAt(),
        entity.getCanceledAt(),
        entity.getMetadataJson(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : "system"
    );
  }
}
