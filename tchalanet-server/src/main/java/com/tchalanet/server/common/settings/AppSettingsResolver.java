package com.tchalanet.server.common.settings;

import com.tchalanet.server.common.persistence.AppSettingEntity;
import com.tchalanet.server.common.persistence.AppSettingRepository;
import com.tchalanet.server.common.settings.dto.ResolvedSettingDto;
import com.tchalanet.server.common.settings.query.ResolveAppSettingsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppSettingsResolver {

    private final AppSettingRepository repo;

    @Cacheable(
        cacheNames = "app_settings_resolved",
        key = "T(com.tchalanet.server.common.settings.AppSettingsCacheKey).of(#q)"
    )
    public List<ResolvedSettingDto> resolve(ResolveAppSettingsQuery q) {
        // (tu peux reprendre celui que je t’ai donné)
        UUID tenantId = Objects.requireNonNull(q.tenantId(), "tenantId");
        List<String> namespaces = (q.namespaces() == null || q.namespaces().isEmpty())
            ? List.of()
            : q.namespaces();

        // map key = namespace + '\u0000' + settingKey
        Map<String, ResolvedSettingDto> resolved = new LinkedHashMap<>();

        // 1) GLOBAL
        merge(resolved, fetchGlobal(namespaces), "GLOBAL");

        // 2) TENANT
        merge(resolved, fetchTenant(tenantId, namespaces), "TENANT");

        // 3) OUTLET (si fourni)
        if (q.outletId() != null) {
            merge(resolved, fetchOutlet(tenantId, q.outletId(), namespaces), "OUTLET");
        }

        // 4) TERMINAL (si fourni)
        if (q.terminalId() != null) {
            merge(resolved, fetchTerminal(tenantId, q.terminalId(), namespaces), "TERMINAL");
        }

        return List.copyOf(resolved.values());
    }

    private List<AppSettingEntity> fetchGlobal(List<String> namespaces) {
        if (namespaces.isEmpty()) return List.of();
        return repo.findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(AppSettingLevel.GLOBAL, namespaces);
    }

    private List<AppSettingEntity> fetchTenant(UUID tenantId, List<String> namespaces) {
        if (namespaces.isEmpty()) return List.of();
        return repo.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndNamespaceIn(
            AppSettingLevel.TENANT, tenantId, namespaces);
    }

    private List<AppSettingEntity> fetchOutlet(UUID tenantId, UUID outletId, List<String> namespaces) {
        if (namespaces.isEmpty()) return List.of();
        return repo.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndNamespaceIn(
            AppSettingLevel.OUTLET, tenantId, outletId, namespaces);
    }

    private List<AppSettingEntity> fetchTerminal(UUID tenantId, UUID terminalId, List<String> namespaces) {
        if (namespaces.isEmpty()) return List.of();
        return repo.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndTerminalIdAndNamespaceIn(
            AppSettingLevel.TERMINAL, tenantId, terminalId, namespaces);
    }

    private void merge(
        Map<String, ResolvedSettingDto> resolved,
        List<AppSettingEntity> entities,
        String effectiveLevel
    ) {
        for (var e : entities) {
            String k = keyOf(e.getNamespace(), e.getSettingKey());
            // override allowed: later calls overwrite earlier ones
            resolved.put(k, new ResolvedSettingDto(
                e.getNamespace(),
                e.getSettingKey(),
                safeType(e.getValueType()),
                e.getSettingValue(),
                effectiveLevel
            ));
        }
    }

    private static String keyOf(String ns, String key) {
        return ns + "\u0000" + key;
    }

    private static AppSettingValueType safeType(Object raw) {
        if (raw instanceof AppSettingValueType t) return t;
        if (raw == null) return AppSettingValueType.STRING;
        try {
            return AppSettingValueType.valueOf(raw.toString());
        } catch (Exception ex) {
            return AppSettingValueType.STRING;
        }
    }
}
