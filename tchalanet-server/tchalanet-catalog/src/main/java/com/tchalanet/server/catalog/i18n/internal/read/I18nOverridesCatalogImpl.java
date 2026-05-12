package com.tchalanet.server.catalog.i18n.internal.read;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nGlobalKeyStatsView;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.api.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.catalog.i18n.internal.cache.I18nOverridesCacheNames;
import com.tchalanet.server.catalog.i18n.internal.mapper.I18nOverrideMapper;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideEntity;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideRepository;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.security.ApiScope;
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

import java.util.*;

/**
 * I18n Overrides Catalog Implementation (READ SIDE)
 * <p>
 * Safety note:
 * - In PLATFORM scope with SUPER_ADMIN, RLS SELECT may allow cross-tenant reads.
 * - Therefore, resolveLocale(locale) MUST NOT merge TENANT-level overrides implicitly,
 * otherwise it can accidentally merge tenant overrides from multiple tenants.
 * <p>
 * Rules:
 * - resolveLocale(locale, ctx): GLOBAL + (TENANT only if not PLATFORM scope)
 * - resolveLocaleForTenant(locale, tenantId): GLOBAL + TENANT for the specified tenant (explicit)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class I18nOverridesCatalogImpl implements I18nOverridesCatalog {

    private final I18nOverrideRepository repository;
    private final I18nOverrideMapper mapper;

    // ------------------------------------------------------------
    // Resolve (safe)
    // ------------------------------------------------------------

    @Override
    @Cacheable(
        value = I18nOverridesCacheNames.RESOLVED_BY_LOCALE,
        key =
            // tenantUuid can be null (platform/global resolve)
            "(#ctx == null || #ctx.tenantUuid() == null ? '__none__' : #ctx.tenantUuid().toString())"
                + " + ':' + #locale"
                + " + ':' + (#ctx == null ? '__noctx__' : (#ctx.apiScope() == null ? '__noscope__' : #ctx.apiScope().name()))"
                + " + ':' + (#ctx == null ? '__novis__' : (#ctx.deletedVisibilitySafe() == null ? '__novis__' : #ctx.deletedVisibilitySafe()))")
    public Map<String, String> resolveLocale(String locale, @CurrentContext TchRequestContext ctx) {
        if (locale == null || locale.isBlank()) return Map.of();
        String loc = locale.trim();

        // Always include GLOBAL
        List<I18nOverrideEntity> globals =
            repository.findByLocaleAndLevelAndActiveTrueAndDeletedAtIsNull(
                loc, I18nOverrideLevel.GLOBAL);

        // Never implicitly merge TENANT overrides in PLATFORM scope.
        boolean allowTenantMerge =
            ctx != null
                && ctx.tenantUuid() != null
                && !isPlatformScope(ctx);

        List<I18nOverrideEntity> tenants =
            allowTenantMerge
                ? repository.findByLocaleAndLevelAndActiveTrueAndDeletedAtIsNull(
                loc, I18nOverrideLevel.TENANT)
                : List.of();

        return merge(globals, tenants);
    }

    /**
     * Explicit resolve for a limitScopeRef tenant (platform admin use-case).
     * Returns GLOBAL + TENANT (filtered by tenantId).
     */
    @Override
    public Map<String, String> resolveLocaleForTenant(String locale, TenantId tenantId) {
        if (locale == null || locale.isBlank() || tenantId == null) return Map.of();
        String loc = locale.trim();

        List<I18nOverrideEntity> globals =
            repository.findByLocaleAndLevelAndActiveTrueAndDeletedAtIsNull(
                loc, I18nOverrideLevel.GLOBAL);

        // IMPORTANT: explicit tenant filter, safe even in PLATFORM cross-tenant SELECT.
        List<I18nOverrideEntity> tenants =
            repository.findByLocaleAndLevelAndTenantIdAndActiveTrueAndDeletedAtIsNull(
                loc, I18nOverrideLevel.TENANT, tenantId.value());

        return merge(globals, tenants);
    }

    private Map<String, String> merge(List<I18nOverrideEntity> globals, List<I18nOverrideEntity> tenants) {
        Map<String, String> merged = new LinkedHashMap<>(Math.max(16, globals.size() + tenants.size()));

        for (var e : globals) {
            putIfValid(merged, e);
        }
        for (var e : tenants) {
            putIfValid(merged, e); // overwrite global
        }

        return Map.copyOf(merged);
    }

    private static void putIfValid(Map<String, String> out, I18nOverrideEntity e) {
        if (e.getI18nKey() == null || e.getI18nValue() == null) return;
        var k = e.getI18nKey().trim();
        var v = e.getI18nValue();
        if (!k.isBlank()) out.put(k, v);
    }

    private boolean isPlatformScope(TchRequestContext ctx) {
        var s = ctx.apiScope();
        return ApiScope.PLATFORM == s;
    }

    // ------------------------------------------------------------
    // Search (unchanged, but now PLATFORM superadmin can see cross-tenant)
    // ------------------------------------------------------------

    @Override
    public TchPage<I18nOverrideView> search(SearchI18nOverridesCriteria criteria, TchPageRequest pageRequest) {
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

        // Prefer TENANT over GLOBAL (RLS makes TENANT rows visible only for current tenant in tenant/admin scopes)
        var tenant =
            repository.findFirstByLocaleAndI18nKeyAndLevel(loc, key, I18nOverrideLevel.TENANT);
        if (tenant.isPresent()) return tenant.map(mapper::toView);

        var global =
            repository.findFirstByLocaleAndI18nKeyAndLevel(loc, key, I18nOverrideLevel.GLOBAL);
        return global.map(mapper::toView);
    }

    // ------------------------------------------------------------
    // Specification builder
    // ------------------------------------------------------------

    private Specification<I18nOverrideEntity> buildSpecification(SearchI18nOverridesCriteria criteria) {
        Specification<I18nOverrideEntity> spec = (root, query, cb) -> {
            // reference 'query' to avoid static-analysis warnings about unused parameters
            query.getRoots();
            return cb.conjunction();
        };

        // deleted visibility (defaults to active)
        String vis = criteria != null ? criteria.visibilitySafe() : "active";
        if ("active".equals(vis)) {
            spec = spec.and((root, query, cb) -> {
                query.getRoots();
                return cb.isNull(root.get("deletedAt"));
            });
        } else if ("deleted".equals(vis)) {
            spec = spec.and((root, query, cb) -> {
                query.getRoots();
                return cb.isNotNull(root.get("deletedAt"));
            });
        }

        if (criteria == null) return spec;

        // level filter
        if (criteria.level() != null) {
            spec = spec.and((root, query, cb) -> {
                query.getRoots();
                return cb.equal(root.get("level"), criteria.level());
            });
        }

        // locale filter
        if (criteria.locale() != null && !criteria.locale().isBlank()) {
            spec = spec.and((root, query, cb) -> {
                query.getRoots();
                return cb.equal(root.get("locale"), criteria.locale().trim());
            });
        }

        // key contains filter
        if (criteria.i18nKeyContains() != null && !criteria.i18nKeyContains().isBlank()) {
            String like = "%" + criteria.i18nKeyContains().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                query.getRoots();
                return cb.like(cb.lower(root.get("i18nKey")), like);
            });
        }

        // active filter (optional)
        if (criteria.active() != null) {
            spec = spec.and((root, query, cb) -> {
                query.getRoots();
                return cb.equal(root.get("active"), criteria.active());
            });
        }

        // Explicit tenant filter when provided in criteria (useful for PLATFORM admin queries)
        if (criteria.tenantId() != null) {
            spec = spec.and((root, q, cb) -> {
                q.getRoots();
                return cb.equal(root.get("tenantId"), criteria.tenantId());
            });
        }

        return spec;
    }

    @Override
    public I18nGlobalKeyStatsView keyStats() {
        // Count distinct keys and locales among GLOBAL active overrides
        List<I18nOverrideEntity> globals = repository.findByLevelAndActiveTrueAndDeletedAtIsNull(I18nOverrideLevel.GLOBAL);

        int totalOverrides = globals.size();
        // distinct keys
        long totalKeys = globals.stream().map(I18nOverrideEntity::getI18nKey).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().count();
        // distinct locales
        long totalLocales = globals.stream().map(I18nOverrideEntity::getLocale).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().count();

        return new I18nGlobalKeyStatsView((int) totalKeys, (int) totalLocales, totalOverrides);
    }
}
