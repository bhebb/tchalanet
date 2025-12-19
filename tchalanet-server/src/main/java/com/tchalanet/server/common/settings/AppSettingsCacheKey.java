package com.tchalanet.server.common.settings;

import com.tchalanet.server.common.settings.query.ResolveAppSettingsQuery;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AppSettingsCacheKey {

    private AppSettingsCacheKey() {
    }

    public static String of(ResolveAppSettingsQuery q) {
        Objects.requireNonNull(q.tenantId(), "tenantId");

        List<String> ns = (q.namespaces() == null) ? List.of() : q.namespaces();
        String nsKey = ns.stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .sorted()
            .collect(Collectors.joining(","));

        return "t=" + q.tenantId()
            + "|o=" + (q.outletId() == null ? "-" : q.outletId())
            + "|m=" + (q.terminalId() == null ? "-" : q.terminalId())
            + "|ns=" + nsKey;
    }
}
