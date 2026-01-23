package com.tchalanet.server.catalog.billing.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.billing.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.catalog.billing.application.port.out.SubscriptionWriterPort;
import com.tchalanet.server.catalog.billing.domain.model.Subscription;
import com.tchalanet.server.catalog.billing.domain.model.SubscriptionStatus;
import com.tchalanet.server.catalog.billing.infra.persistence.mapper.SubscriptionPersistenceMapper;
import com.tchalanet.server.catalog.billing.infra.persistence.repo.SubscriptionJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionJpaRepositoryAdapter
    implements SubscriptionReaderPort, SubscriptionWriterPort {

  private final SubscriptionJpaRepository jpa;
  private final SubscriptionPersistenceMapper mapper;

  @Override
  public Optional<Subscription> findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
      TenantId tenantId, List<SubscriptionStatus> statuses) {
    return jpa.findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
            tenantId.uuid(), statuses)
        .map(mapper::toDomain);
  }

  @Override
  public Subscription save(Subscription subscription) {
    var entity = mapper.toEntity(subscription);
    var saved = jpa.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public List<Subscription> findByStatusAndCurrentPeriodEndBefore(
      SubscriptionStatus status, Instant before) {
    return jpa.findByStatusAndCurrentPeriodEndBefore(status, before).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Optional<Subscription> findFirstByTenantIdAndStatus(
      TenantId tenantId, Set<SubscriptionStatus> statuses) {
    return findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
        tenantId, List.copyOf(statuses));
  }
}
