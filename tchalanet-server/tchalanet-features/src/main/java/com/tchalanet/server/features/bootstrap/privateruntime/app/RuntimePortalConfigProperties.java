package com.tchalanet.server.features.bootstrap.privateruntime.app;

import com.tchalanet.server.features.bootstrap.privateruntime.model.RuntimePortalConfigView;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.portal-auth-handoff.portal-base-urls")
public record RuntimePortalConfigProperties(
    String admin,
    String platform
) {
    public RuntimePortalConfigProperties {
        admin = normalize(admin, "/admin");
        platform = normalize(platform, "/platform");
    }

    RuntimePortalConfigView toView() {
        return new RuntimePortalConfigView(Map.of(
            "admin-portal", admin,
            "platform-portal", platform));
    }

    private static String normalize(String value, String fallback) {
        var normalized = value == null || value.isBlank() ? fallback : value.trim();
        return normalized.length() > 1 ? normalized.replaceAll("/+$", "") : normalized;
    }
}
