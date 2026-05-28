package com.tchalanet.server.core.reconciliation.internal.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.reconciliation.daily")
public class ReconciliationDailyJobProperties {

    private boolean active = true;
    private String cron = "0 */5 * * * *";
    private int midnightWindowMinutes = 30;
    private int maxTenantsPerTick = 1000;
}
