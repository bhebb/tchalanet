package com.tchalanet.server.catalog.i18n.internal.read;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.api.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.catalog.i18n.internal.cache.I18nOverridesCacheNames;
import com.tchalanet.server.catalog.i18n.internal.mapper.I18nOverrideMapper;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideEntity;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideRepository;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * I18n Overrides Catalog Implementation (READ SIDE)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class I18nOverridesCatalogImpl implements I18nOverridesCatalog {

    private final I18nOverrideRepository repository;
    private final I18nOverrideMapper mapper;

    @Override
    @Cacheable(
        value = I18nOverridesCacheNames.RESOLVED_BY_LOCALE,
        key = "(#ctx.tenantIdSafe() == null ? '__none__' : #ctx.tenantIdSafe().value()) + ':' + #locale"
    )
    public Map<String, String> resolveLocale(String locale, @CurrentContext TchRequestContext ctx) {
        if (locale == null || locale.isBlank()) return Map.of();
        String loc = locale.trim();

        // Load GLOBAL then TENANT; tenant overwrites global on same i18n_key
        List<com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideEntity> globals =
            repository.findByLocaleAndLevelAndActiveTrueAndDeletedAtIsNull(loc, I18nOverrideLevel.GLOBAL);

        List<com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideEntity> tenants =
            repository.findByLocaleAndLevelAndActiveTrueAndDeletedAtIsNull(loc, I18nOverrideLevel.TENANT);

        Map<String, String> merged = new LinkedHashMap<>(Math.max(16, globals.size() + tenants.size()));

        for (var e : globals) {
            if (e.getI18nKey() != null && e.getI18nValue() != null) {
                merged.put(e.getI18nKey(), e.getI18nValue());
            }
        }
        for (var e : tenants) {
            if (e.getI18nKey() != null && e.getI18nValue() != null) {
                merged.put(e.getI18nKey(), e.getI18nValue()); // overwrite
            }
        }

        return Map.copyOf(merged);
    }

    /**
     * Search i18n overrides with pagination and filtering.
     *
     * @param criteria    search criteria
     * @param pageRequest pagination parameters
     * @return page of overrides
     */
    @Override
    @Transactional(readOnly = true)
    public TchPage<I18nOverrideView> search(
        SearchI18nOverridesCriteria criteria, TchPageRequest pageRequest) {
        Specification<I18nOverrideEntity> spec = buildSpecification(criteria);

        PageRequest springPageRequest =
            PageRequest.of(
                pageRequest.pageable().getPageNumber(),
                pageRequest.pageable().getPageSize(),
                pageRequest.pageable().getSort());

        Page<I18nOverrideEntity> page = repository.findAll(spec, springPageRequest);

        return TchPage.of(
            mapper.toViews(page.getContent()),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast(),
            page.hasNext(),
            page.hasPrevious());
    }

    @Override
    public Optional<I18nOverrideView> findByKey(String locale, String i18nKey) {
        if (locale == null || locale.isBlank() || i18nKey == null || i18nKey.isBlank()) return Optional.empty();
        String loc = locale.trim();
        String key = i18nKey.trim();

        // Prefer TENANT over GLOBAL (RLS makes TENANT rows visible only for current tenant)
        var tenant = repository.findFirstByLocaleAndI18nKeyAndLevel(
            loc, key, I18nOverrideLevel.TENANT);
        if (tenant.isPresent()) return tenant.map(mapper::toView);

        var global = repository.findFirstByLocaleAndI18nKeyAndLevel(
            loc, key, I18nOverrideLevel.GLOBAL);
        return global.map(mapper::toView);
    }


    // ========================================
    // Search specification builder
    // ========================================

    private Specification<I18nOverrideEntity> buildSpecification(SearchI18nOverridesCriteria criteria) {
        Specification<I18nOverrideEntity> spec = (root, query, cb) -> cb.conjunction();

        // soft-delete
        spec = spec.and((root, query, cb) -> cb.isNull(root.get("deletedAt")));

        // level filter
        if (criteria.level() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("level"), criteria.level()));
        }

        // locale filter
        if (criteria.locale() != null && !criteria.locale().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("locale"), criteria.locale().trim()));
        }

        // key contains filter
        if (criteria.i18nKeyContains() != null && !criteria.i18nKeyContains().isBlank()) {
            String like = "%" + criteria.i18nKeyContains().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("i18nKey")), like));
        }

        // active filter (optional)
        if (criteria.active() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), criteria.active()));
        }

        return spec;
    }

}
