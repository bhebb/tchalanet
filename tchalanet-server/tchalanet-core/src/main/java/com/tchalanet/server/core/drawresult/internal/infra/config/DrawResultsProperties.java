package com.tchalanet.server.core.drawresult.internal.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.draw.results")
public class DrawResultsProperties {

    private boolean active = true;
    private Scheduler scheduler = new Scheduler();
    private Limits limits = new Limits();
    private Defaults defaults = new Defaults();
    private Notifications notifications = new Notifications();
    private Settlement settlement = new Settlement();

    @Getter
    @Setter
    public static class Scheduler {
        private boolean active = true;
        private String cron = "0 */5 * * * *";

        private int minMinutesAfterDraw = 3;
        private int maxMinutesAfterDraw = 120;
        private int cooldownMinutes = 10;
    }

    @Getter
    @Setter
    public static class Limits {
        private int maxSlotsPerTick = 100;
        private int hardDaysBack = 7;
    }

    @Getter
    @Setter
    public static class Defaults {
        private int manualDaysBack = 0;
        private int manualMaxSlots = 50;
    }

    @Getter
    @Setter
    public static class Notifications {
        private Slack slack = new Slack();
    }

    @Getter
    @Setter
    public static class Slack {
        private boolean enabled = false;
        private String channel = "draw-results";
        private String priority = "LOW";
    }

    @Getter
    @Setter
    public static class Settlement {
        private boolean active = true;
        private String cron = "0 */5 * * * *";
        private int daysBack = 1;
        private int maxDrawsPerTenant = 500;
    }
}
