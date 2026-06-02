package com.tchalanet.server.platform.tenant.internal.adapter;

import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatus;
import com.tchalanet.server.catalog.tenant.api.model.TenantType;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.cache.TenantCacheNames;
import com.tchalanet.server.platform.tenant.internal.resolver.TenantBootstrapRow;
import com.tchalanet.server.platform.tenant.internal.resolver.TenantRegistryReader;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing {@link TenantPreContextLookupApi}.
 * Single source of truth for pre-RLS tenant reads — backed by rawDataSource JDBC.
 */
@Component
@RequiredArgsConstructor
public class TenantRegistryApiAdapter implements TenantPreContextLookupApi {

    private final TenantRegistryReader reader;

    @Override
    @Cacheable(cacheNames = TenantCacheNames.REGISTRY_BY_CODE, key = "#codeLower",
        unless = "#result == null || !#result.isPresent()")
    public Optional<TenantRegistryView> findByCode(String codeLower) {
        return reader.findByCode(codeLower).map(this::toView);
    }

    @Override
    @Cacheable(cacheNames = TenantCacheNames.REGISTRY_BY_ID, key = "#tenantId.value()",
        unless = "#result == null || !#result.isPresent()")
    public Optional<TenantRegistryView> findById(TenantId tenantId) {
        return reader.findById(tenantId.value()).map(this::toView);
    }

    @Override
    @Cacheable(cacheNames = TenantCacheNames.ACTIVE_TENANT_IDS)
    public List<TenantId> listActiveTenantIds() {
        return reader.listActiveTenantIds().stream().map(TenantId::of).toList();
    }

    @Override
    public TchPage<TenantRegistryView> listTenants(TchPageRequest pageRequest) {
        var pageable = pageRequest.pageable();
        long total = reader.countAll();
        var views = reader.listAll(pageable.getPageSize(), (int) pageable.getOffset(), buildOrderBy(pageable))
            .stream().map(this::toView).toList();
        int totalPages = pageable.getPageSize() == 0 ? 1 : (int) Math.ceil((double) total / pageable.getPageSize());
        boolean isLast = pageable.getPageNumber() >= totalPages - 1;
        return new TchPage<>(views, pageable.getPageNumber(), pageable.getPageSize(),
            total, totalPages, isLast, !isLast, pageable.getPageNumber() > 0);
    }

    private TenantRegistryView toView(TenantBootstrapRow row) {
        return new TenantRegistryView(
            TenantId.of(row.id()),
            row.code(),
            row.name(),
            safeStatus(row.status()),
            safeType(row.type()),
            safeZoneId(row.timezone()),
            safeCurrency(row.currency()),
            safe(row.defaultLanguage(), "fr"),
            safe(row.defaultLocale(), "fr-HT"),
            row.addressId() != null ? Optional.of(AddressId.of(row.addressId())) : Optional.empty(),
            row.activeThemeId() != null ? Optional.of(ThemePresetId.of(row.activeThemeId())) : Optional.empty());
    }

    private String buildOrderBy(org.springframework.data.domain.Pageable pageable) {
        if (pageable.getSort().isUnsorted()) return "created_at DESC";
        var sb = new StringBuilder();
        pageable.getSort().forEach(o -> {
            if (sb.length() > 0) sb.append(", ");
            sb.append(switch (o.getProperty()) {
                case "createdAt" -> "created_at";
                case "code" -> "code";
                case "name" -> "name";
                case "status" -> "status";
                default -> "created_at";
            }).append(o.isAscending() ? " ASC" : " DESC");
        });
        return sb.toString();
    }

    private ZoneId safeZoneId(String raw) {
        try { return raw == null || raw.isBlank() ? ZoneOffset.UTC : ZoneId.of(raw); }
        catch (Exception e) { return ZoneOffset.UTC; }
    }

    private Currency safeCurrency(String raw) {
        try { return raw == null || raw.isBlank() ? Currency.getInstance("USD") : Currency.getInstance(raw); }
        catch (Exception e) { return Currency.getInstance("USD"); }
    }

    private TenantStatus safeStatus(String raw) {
        try { return raw == null ? TenantStatus.DRAFT : TenantStatus.valueOf(raw); }
        catch (Exception e) { return TenantStatus.DRAFT; }
    }

    private TenantType safeType(String raw) {
        try { return raw == null ? TenantType.BORLETTE : TenantType.valueOf(raw); }
        catch (Exception e) { return TenantType.BORLETTE; }
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
