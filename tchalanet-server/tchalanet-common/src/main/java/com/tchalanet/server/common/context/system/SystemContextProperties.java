package com.tchalanet.server.common.context.system;

import com.tchalanet.server.common.types.id.UserId;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.system")
public record SystemContextProperties(
    UUID userId,
    String tenantCode,
    String actorName
) {

    public UserId systemUserId() {
        return UserId.of(userId);
    }

    public String normalizedTenantCode() {
        return normalize(tenantCode);
    }

    public String normalizedActorName() {
        var normalized = normalize(actorName);
        return normalized == null ? "system" : normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
