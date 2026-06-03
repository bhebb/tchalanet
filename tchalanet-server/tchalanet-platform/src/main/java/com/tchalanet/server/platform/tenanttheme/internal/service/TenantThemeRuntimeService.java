package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenanttheme.api.model.ThemeRuntimeView;
import com.tchalanet.server.platform.tenanttheme.internal.persistence.TenantThemePersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TenantThemeRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(TenantThemeRuntimeService.class);

    private final ThemeCatalog themeCatalog;
    private final TenantThemePersistenceAdapter persistence;
    private final TenantThemeFallbackService fallback;

    @Transactional(readOnly = true)
    public ThemeRuntimeView getRuntime(TenantId tenantId, String requestedMode) {
        String presetCode;
        String defaultMode;
        long version;
        boolean isDefault;

        if (tenantId == null) {
            // No tenant context (unauthenticated public call) — serve global default
            presetCode = fallback.resolveFallback(null, null);
            defaultMode = "SYSTEM";
            version = 0L;
            isDefault = true;
        } else {
            var tenantTheme = persistence.findActiveByTenantId(tenantId);
            if (tenantTheme.isPresent()) {
                var t = tenantTheme.get();
                presetCode = t.presetCode();
                defaultMode = t.defaultMode();
                version = t.version();
                isDefault = t.isDefault();
            } else {
                presetCode = fallback.resolveFallback(tenantId, null);
                defaultMode = "SYSTEM";
                version = 0L;
                isDefault = true;
            }
        }

        var preset = themeCatalog.findByCode(presetCode);
        if (preset.isEmpty() || !preset.get().active()) {
            presetCode = fallback.resolveFallback(tenantId, presetCode);
            preset = themeCatalog.findByCode(presetCode);
        }

        String resolvedMode = resolveMode(requestedMode, defaultMode);
        Map<String, String> tokens = extractTokens(preset.map(p -> p.config()).orElse(null), resolvedMode);

        return new ThemeRuntimeView(presetCode, resolvedMode, tokens, isDefault, version);
    }

    private String resolveMode(String requested, String defaultMode) {
        if (requested != null && (requested.equalsIgnoreCase("light") || requested.equalsIgnoreCase("dark"))) {
            return requested.toLowerCase();
        }
        if ("LIGHT".equalsIgnoreCase(defaultMode)) return "light";
        if ("DARK".equalsIgnoreCase(defaultMode)) return "dark";
        return "light"; // SYSTEM fallback → light
    }

    private Map<String, String> extractTokens(JsonNode config, String mode) {
        if (config == null || config.isNull()) return Collections.emptyMap();
        try {
            var tokensNode = config.path("tokens").path(mode);
            if (tokensNode.isMissingNode() || tokensNode.isNull()) return Collections.emptyMap();
            var result = new HashMap<String, String>();
            tokensNode.properties().forEach(e -> result.put(e.getKey(), e.getValue().asText()));
            return Collections.unmodifiableMap(result);
        } catch (Exception e) {
            log.warn("Failed to extract theme tokens for mode={}: {}", mode, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
