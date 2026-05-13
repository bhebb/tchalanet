package com.tchalanet.server.common.context.system;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.system")
public record SystemContextProperties(
    UUID userId,
    UUID tenantId,
    String tenantCode,
    String actorName
) {

    public UserId systemUserId() {
        return UserId.of(userId);
    }

    public TenantId systemTenantId() {
        return TenantId.of(tenantId);
    }
}
