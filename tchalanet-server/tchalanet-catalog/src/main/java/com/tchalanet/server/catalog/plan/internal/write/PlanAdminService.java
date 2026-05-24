package com.tchalanet.server.catalog.plan.internal.write;

import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.catalog.plan.internal.cache.PlanCacheNames;
import com.tchalanet.server.catalog.plan.internal.mapper.PlanMapper;
import com.tchalanet.server.catalog.plan.internal.persistence.PlanJpaEntity;
import com.tchalanet.server.catalog.plan.internal.persistence.PlanJpaRepository;
import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.common.web.error.ProblemRest;
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
    private final PlanFeatureJsonValidator featureValidator;
    private final PlanLimitJsonValidator limitValidator;

    @Transactional
    @CacheEvict(
        cacheNames = {
            PlanCacheNames.ACTIVE_PLANS,
            PlanCacheNames.PLAN_BY_CODE,
            PlanCacheNames.PLAN_BY_ID
        },
        allEntries = true
    )
    public PlanView create(PlanCreateRequest req) {
        featureValidator.parseAndValidate(req.featuresJson());
        limitValidator.parseAndValidate(req.limitsJson());

        var entity = new PlanJpaEntity();
        entity.setCode(req.code());
        entity.setName(req.name());
        entity.setDescription(req.description());
        entity.setPriceAmount(req.priceAmount() != null ? req.priceAmount() : BigDecimal.ZERO);
        entity.setCurrency(req.currency() != null ? req.currency() : "USD");
        entity.setBillingPeriod(req.billingPeriod() != null ? req.billingPeriod() : "MONTHLY");
        entity.setLimitsJson(normalizeJson(req.limitsJson()));
        entity.setFeaturesJson(normalizeJson(req.featuresJson()));
        entity.setActive(req.active() != null ? req.active() : true);
        entity.setDefaultPlan(req.isDefault() != null ? req.isDefault() : false);

        return mapper.toView(repository.save(entity));
    }

    @Transactional
    @CacheEvict(
        cacheNames = {
            PlanCacheNames.ACTIVE_PLANS,
            PlanCacheNames.PLAN_BY_CODE,
            PlanCacheNames.PLAN_BY_ID
        },
        allEntries = true
    )
    public PlanView updateMetadata(PlanId id, PlanMetadataUpdateRequest req) {
        var entity = getRequired(id);

        if (req.name() != null) entity.setName(req.name());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.priceAmount() != null) entity.setPriceAmount(req.priceAmount());
        if (req.currency() != null) entity.setCurrency(req.currency());
        if (req.billingPeriod() != null) entity.setBillingPeriod(req.billingPeriod());

        return mapper.toView(repository.save(entity));
    }

    @Transactional
    @CacheEvict(
        cacheNames = {
            PlanCacheNames.ACTIVE_PLANS,
            PlanCacheNames.PLAN_BY_CODE,
            PlanCacheNames.PLAN_BY_ID
        },
        allEntries = true
    )
    public PlanView replaceFeatures(PlanId id, PlanFeaturesUpdateRequest req) {
        var entity = getRequired(id);

        featureValidator.parseAndValidate(req.featuresJson());
        entity.setFeaturesJson(normalizeJson(req.featuresJson()));

        return mapper.toView(repository.save(entity));
    }

    @Transactional
    @CacheEvict(
        cacheNames = {
            PlanCacheNames.ACTIVE_PLANS,
            PlanCacheNames.PLAN_BY_CODE,
            PlanCacheNames.PLAN_BY_ID
        },
        allEntries = true
    )
    public PlanView replaceLimits(PlanId id, PlanLimitsUpdateRequest req) {
        var entity = getRequired(id);

        limitValidator.parseAndValidate(req.limitsJson());
        entity.setLimitsJson(normalizeJson(req.limitsJson()));

        return mapper.toView(repository.save(entity));
    }

    @Transactional
    @CacheEvict(
        cacheNames = {
            PlanCacheNames.ACTIVE_PLANS,
            PlanCacheNames.PLAN_BY_CODE,
            PlanCacheNames.PLAN_BY_ID
        },
        allEntries = true
    )
    public void deactivate(PlanId id) {
        var entity = getRequired(id);
        entity.setActive(false);
        repository.save(entity);
    }

    @Transactional
    @CacheEvict(
        cacheNames = {
            PlanCacheNames.ACTIVE_PLANS,
            PlanCacheNames.PLAN_BY_CODE,
            PlanCacheNames.PLAN_BY_ID
        },
        allEntries = true
    )
    public void softDelete(PlanId id) {
        var entity = getRequired(id);
        entity.setDeletedAt(Instant.now());
        entity.setActive(false);
        repository.save(entity);
    }

    private PlanJpaEntity getRequired(PlanId id) {
        return repository.findById(id.value())
            .orElseThrow(() -> ProblemRest.notFound("plan", id));
    }

    private String normalizeJson(String raw) {
        return raw == null || raw.isBlank() ? "{}" : raw;
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

    public record PlanMetadataUpdateRequest(
        String name,
        String description,
        BigDecimal priceAmount,
        String currency,
        String billingPeriod
    ) {}

    public record PlanFeaturesUpdateRequest(
        String featuresJson
    ) {}

    public record PlanLimitsUpdateRequest(
        String limitsJson
    ) {}
}
