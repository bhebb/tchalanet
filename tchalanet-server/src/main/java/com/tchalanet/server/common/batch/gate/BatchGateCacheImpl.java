package com.tchalanet.server.common.batch.gate;

import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BatchGateCacheImpl implements BatchGateCache {

    private static final String NAMESPACE = "batch";

    private final BatchFlagCache flagCache;
    private final SettingRepository settingRepo;

    @Override
    public Optional<Boolean> getTenant(JobKey jobKey, TenantId tenantId) {
        String cacheKey = NAMESPACE + ":t:" + tenantId.value() + ":" + jobKey.value();
        return Optional.ofNullable(
            flagCache.getBool(cacheKey, () -> loadFromSettings(SettingLevel.TENANT, tenantId, jobKey))
        );
    }

    @Override
    public Optional<Boolean> getGlobal(JobKey jobKey) {
        String cacheKey = NAMESPACE + ":g:" + jobKey.value();
        return Optional.ofNullable(
            flagCache.getBool(cacheKey, () -> loadFromSettings(SettingLevel.GLOBAL, null, jobKey))
        );
    }

    @Override
    public void putTenant(JobKey jobKey, TenantId tenantId, boolean enabled) {
        // cache only
        String cacheKey = NAMESPACE + ":t:" + tenantId.value() + ":" + jobKey.value();
        flagCache.put(cacheKey, enabled);
    }

    @Override
    public void putGlobal(JobKey jobKey, boolean enabled) {
        // cache only
        String cacheKey = NAMESPACE + ":g:" + jobKey.value();
        flagCache.put(cacheKey, enabled);
    }

    private Boolean loadFromSettings(SettingLevel level, TenantId tenantId, JobKey jobKey) {
        String settingKey = "jobs." + jobKey.value() + ".enabled";

        return settingRepo
            .findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndTerminalIdAndNamespaceAndSettingKey(
                level,
                tenantId != null ? tenantId.value() : null,
                null,
                null,
                NAMESPACE,
                settingKey)
            .map(e -> parseBoolOrNull(e.getSettingValue()))
            .orElse(null); // no row => no override
    }

    private static Boolean parseBoolOrNull(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase();
        return switch (v) {
            case "true", "1", "yes" -> Boolean.TRUE;
            case "false", "0", "no" -> Boolean.FALSE;
            default -> null; // invalid value => ignore override
        };
    }
}
