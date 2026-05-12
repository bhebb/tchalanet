package com.tchalanet.server.core.uslottery.internal.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.us-lottery")
public class UsLotteryProperties {

    private boolean enabled = true;

    private Map<String, ProviderProperties> providers;
    private CommonProperties common;

    @Getter
    @Setter
    public static class CommonProperties {
        private List<String> holidays;
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
        private Map<String, String> headers; // for TX, etc
        private List<String> holidays;
    }
}
