package com.tchalanet.server.catalog.i18n.internal.read;

import com.tchalanet.server.catalog.i18n.api.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.internal.cache.I18nOverridesCacheNames;
import com.tchalanet.server.catalog.i18n.internal.mapper.I18nOverrideMapper;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideEntity;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideRepository;
import com.tchalanet.server.catalog.i18n.internal.web.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.common.types.id.I18nOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * I18n Overrides Catalog Implementation (READ SIDE)
 *
 * <p>Implements the I18nOverridesCatalog contract with caching.
 *
 * <p>This is the ONLY implementation of read operations. It MUST NOT perform writes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class I18nOverridesCatalogImpl implements I18nOverridesCatalog {

    private final I18nOverrideRepository repository;
    private final I18nOverrideMapper mapper;

    /**
     * Search i18n overrides with pagination and filtering.
     *
     * @param criteria    search criteria
     * @param pageRequest pagination parameters
     * @return page of overrides
     */
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
    @Cacheable(value = I18nOverridesCacheNames.BY_ID, key = "#id.value()")
    public Optional<I18nOverrideView> findById(I18nOverrideId id) {
        return repository.findByIdAndDeletedAtIsNull(id.value()).map(mapper::toView);
    }

    /**
     * Get override by ID.
     *
     * @param id override ID
     * @return override view
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public I18nOverrideView getById(I18nOverrideId id) {
        I18nOverrideEntity entity =
            repository
                .findByIdAndDeletedAtIsNull(id.value())
                .orElseThrow(() -> new IllegalArgumentException("I18n override not found: " + id));
        return mapper.toView(entity);
    }

    @Override
    @Cacheable(
        value = I18nOverridesCacheNames.BY_TENANT_LOCALE,
        key = "#tenantId.value() + ':' + #locale + ':' + #i18nKey")
    public Optional<I18nOverrideView> findByKey(TenantId tenantId, String locale, String i18nKey) {
        log.debug(
            "Loading i18n override for tenant={}, locale={}, key={}", tenantId, locale, i18nKey);
        return repository
            .findFirstByTenantIdAndLocaleAndI18nKeyAndActiveTrueAndDeletedAtIsNull(
                tenantId.value(), locale, i18nKey)
            .map(mapper::toView);
    }

    @Override
    @Cacheable(
        value = I18nOverridesCacheNames.BY_TENANT_LOCALE,
        key = "#tenantId.value() + ':' + #locale")
    public List<I18nOverrideView> listByTenantAndLocale(TenantId tenantId, String locale) {
        log.debug("Loading i18n overrides for tenant={}, locale={}", tenantId, locale);
        var entities =
            repository.findByTenantIdAndLocaleAndActiveTrueAndDeletedAtIsNull(
                tenantId.value(), locale);
        return mapper.toViews(entities);
    }

    @Override
    public Map<String, String> getOverridesMap(TenantId tenantId, String locale) {
        return listByTenantAndLocale(tenantId, locale).stream()
            .collect(Collectors.toMap(I18nOverrideView::i18nKey, I18nOverrideView::i18nValue));
    }

    @Override
    @Cacheable(value = I18nOverridesCacheNames.BY_TENANT, key = "#tenantId.value()")
    public List<I18nOverrideView> listByTenant(TenantId tenantId) {
        log.debug("Loading all i18n overrides for tenant={}", tenantId);
        var entities = repository.findByTenantIdAndActiveTrueAndDeletedAtIsNull(tenantId.value());
        return mapper.toViews(entities);
    }


    // ========================================
    // Search specification builder
    // ========================================

    private Specification<I18nOverrideEntity> buildSpecification(SearchI18nOverridesCriteria criteria) {
        Specification<I18nOverrideEntity> spec = (root, query, cb) -> cb.conjunction();

        // Always filter by active and non-deleted
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")));
        spec = spec.and((root, query, cb) -> cb.isNull(root.get("deletedAt")));

        if (criteria.tenantId() != null) {
            spec =
                spec.and(
                    (root, query, cb) -> cb.equal(root.get("tenantId"), criteria.tenantId().value()));
        }

        if (criteria.locale() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("locale"), criteria.locale()));
        }

        if (criteria.i18nKeyContains() != null) {
            spec =
                spec.and(
                    (root, query, cb) ->
                        cb.like(
                            cb.lower(root.get("i18nKey")),
                            "%" + criteria.i18nKeyContains().toLowerCase() + "%"));
        }

        if (criteria.active() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), criteria.active()));
        }

        return spec;
    }
}
