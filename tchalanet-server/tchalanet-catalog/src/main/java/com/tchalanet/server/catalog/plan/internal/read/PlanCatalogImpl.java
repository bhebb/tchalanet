package com.tchalanet.server.catalog.plan.internal.read;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.catalog.plan.internal.cache.PlanCacheNames;
import com.tchalanet.server.catalog.plan.internal.mapper.PlanMapper;
import com.tchalanet.server.catalog.plan.internal.persistence.PlanJpaRepository;
import com.tchalanet.server.common.types.id.PlanId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of PlanCatalog (read-only, cacheable).
 * Maps to spec requirement P1 (read operations) + P5 (cache).
 * Filters deleted_at IS NULL on all reads.
 */
@Service
@RequiredArgsConstructor
public class PlanCatalogImpl implements PlanCatalog {

  private final PlanJpaRepository repository;
  private final PlanMapper mapper;

  @Override
  @Cacheable(value = PlanCacheNames.ACTIVE_PLANS)
  public List<PlanView> listActive() {
    return repository.findAllByActiveTrueAndDeletedAtIsNull()
        .stream()
        .map(mapper::toView)
        .collect(Collectors.toList());
  }

  @Override
  @Cacheable(value = PlanCacheNames.PLAN_BY_CODE, key = "#code")
  public Optional<PlanView> findByCode(String code) {
    return repository.findFirstByCodeIgnoreCaseAndDeletedAtIsNull(code)
        .map(mapper::toView);
  }

  @Override
  @Cacheable(value = PlanCacheNames.PLAN_BY_ID, key = "#id.value()")
  public Optional<PlanView> findById(PlanId id) {
    return repository.findByIdAndDeletedAtIsNull(id.value())
        .map(mapper::toView);
  }
}
