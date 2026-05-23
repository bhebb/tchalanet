package com.tchalanet.server.catalog.plan.internal.write;

import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.catalog.plan.internal.cache.PlanCacheNames;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.catalog.plan.internal.mapper.PlanMapper;
import com.tchalanet.server.catalog.plan.internal.persistence.PlanJpaEntity;
import com.tchalanet.server.catalog.plan.internal.persistence.PlanJpaRepository;
import com.tchalanet.server.common.types.id.PlanId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Admin service for Plan CRUD (internal write operations).
 * Maps to spec requirement P2 (admin writes).
 * All writes evict relevant caches.
 */
@Service
@RequiredArgsConstructor
public class PlanAdminService {

  private final PlanJpaRepository repository;
  private final PlanMapper mapper;

  @Transactional
  @CacheEvict(cacheNames = {PlanCacheNames.ACTIVE_PLANS, PlanCacheNames.PLAN_BY_CODE, PlanCacheNames.PLAN_BY_ID}, allEntries = true)
  public PlanView create(PlanCreateRequest req) {
    validateJson(req.limitsJson(), "limits");
    validateJson(req.featuresJson(), "features");

    var entity = new PlanJpaEntity();
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setDescription(req.description());
    entity.setPriceAmount(req.priceAmount() != null ? req.priceAmount() : BigDecimal.ZERO);
    entity.setCurrency(req.currency() != null ? req.currency() : "USD");
    entity.setBillingPeriod(req.billingPeriod() != null ? req.billingPeriod() : "MONTHLY");
    entity.setLimitsJson(req.limitsJson());
    entity.setFeaturesJson(req.featuresJson());
    entity.setActive(req.active() != null ? req.active() : true);
    entity.setDefaultPlan(req.isDefault() != null ? req.isDefault() : false);

    var saved = repository.save(entity);
    return mapper.toView(saved);
  }

  @Transactional
  @CacheEvict(cacheNames = {PlanCacheNames.ACTIVE_PLANS, PlanCacheNames.PLAN_BY_CODE, PlanCacheNames.PLAN_BY_ID}, allEntries = true)
  public PlanView update(PlanId id, PlanUpdateRequest req) {
    if (req.limitsJson() != null) validateJson(req.limitsJson(), "limits");
    if (req.featuresJson() != null) validateJson(req.featuresJson(), "features");

    var entity = repository.findById(id.value())
        .orElseThrow(() -> ProblemRest.notFound("plan", id));

    if (req.name() != null) entity.setName(req.name());
    if (req.description() != null) entity.setDescription(req.description());
    if (req.priceAmount() != null) entity.setPriceAmount(req.priceAmount());
    if (req.currency() != null) entity.setCurrency(req.currency());
    if (req.billingPeriod() != null) entity.setBillingPeriod(req.billingPeriod());
    if (req.limitsJson() != null) entity.setLimitsJson(req.limitsJson());
    if (req.featuresJson() != null) entity.setFeaturesJson(req.featuresJson());
    if (req.active() != null) entity.setActive(req.active());
    if (req.isDefault() != null) entity.setDefaultPlan(req.isDefault());

    var saved = repository.save(entity);
    return mapper.toView(saved);
  }

  @Transactional
  @CacheEvict(cacheNames = {PlanCacheNames.ACTIVE_PLANS, PlanCacheNames.PLAN_BY_CODE, PlanCacheNames.PLAN_BY_ID}, allEntries = true)
  public void deactivate(PlanId id) {
    var entity = repository.findById(id.value())
        .orElseThrow(() -> ProblemRest.notFound("plan", id));
    entity.setActive(false);
    repository.save(entity);
  }

  @Transactional
  @CacheEvict(cacheNames = {PlanCacheNames.ACTIVE_PLANS, PlanCacheNames.PLAN_BY_CODE, PlanCacheNames.PLAN_BY_ID}, allEntries = true)
  public void softDelete(PlanId id) {
    var entity = repository.findById(id.value())
        .orElseThrow(() -> ProblemRest.notFound("plan", id));
    entity.setDeletedAt(Instant.now());
    entity.setActive(false);
    repository.save(entity);
  }

  private void validateJson(String json, String context) {
    if (json == null || json.isBlank()) return;
    try {
      var node = mapper.jsonUtils.toJsonNode(json);
      if (node != null && !node.isObject()) {
        throw ProblemRest.badRequest("plan." + context + "_must_be_object");
      }
    } catch (Exception e) {
      throw ProblemRest.badRequest("plan.invalid_" + context + "_json");
    }
  }

  public record PlanCreateRequest(
      String code,
      String name,
      String description,
      BigDecimal priceAmount,
      String currency,
      String billingPeriod,
      String limitsJson,
      String featuresJson,
      Boolean active,
      Boolean isDefault
  ) {}

  public record PlanUpdateRequest(
      String name,
      String description,
      BigDecimal priceAmount,
      String currency,
      String billingPeriod,
      String limitsJson,
      String featuresJson,
      Boolean active,
      Boolean isDefault
  ) {}
}
