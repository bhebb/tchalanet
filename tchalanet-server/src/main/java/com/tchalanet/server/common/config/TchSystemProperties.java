package com.tchalanet.server.common.config;

import com.tchalanet.server.common.types.id.UserId;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.system")
public record TchSystemProperties(UUID userId) {

    public UserId systemUserId() {
        return UserId.of(userId);
    }
}
