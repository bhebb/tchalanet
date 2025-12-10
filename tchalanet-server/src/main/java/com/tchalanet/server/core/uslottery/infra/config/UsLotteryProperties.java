package com.tchalanet.server.core.uslottery.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Properties binding for US lottery providers. Mirrors YAML structure under `tch.us-lottery.providers`.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "tch.us-lottery")
public class UsLotteryProperties {

    private Map<String, ProviderProperties> providers;

    /** Default tenant id to associate external US draws when tenant not provided. */
    private String defaultTenantId = "00000000-0000-0000-0000-000000000002";

    /** TTL (seconds) used to mark a fetch attempt in the sync state (defaults to 1 hour). */
    private int syncTtlSeconds = 3600;

    public UUID getDefaultTenantUuid() {
        return UUID.fromString(defaultTenantId);
    }

    @Getter
    @Setter
    public static class ProviderProperties {
        private boolean enabled = true;
        private String baseUrl;
        private String appToken;
        private String timezone;
        private String latestPath;
        private String alertPath;
        private List<GameProps> games;
    }

    @Getter
    @Setter
    public static class GameProps {
        private String code;
        private String externalKey;
        private String drawTime;
    }
}
