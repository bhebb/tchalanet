package com.tchalanet.server.core.draw.internal.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;
import java.time.ZoneId;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.draw")
public class DrawProperties {

    private Cache cache = new Cache();
    private Scheduler scheduler = new Scheduler();
    private Lifecycle lifecycle = new Lifecycle();
    private Settlement settlement = new Settlement();
    private Watchdog watchdog = new Watchdog();


    @Getter
    @Setter
    public static class Cache {
        private Ttl ttl = new Ttl();

        @Getter
        @Setter
        public static class Ttl {
            private int last7m = 5;
            private int todaym = 5;
            private int nexts = 60;
        }
    }

    @Getter
    @Setter
    public static class Windows {
        private boolean enabled = true;
        private ZoneId timezone = ZoneId.of("America/New_York");
        private String fetchResults = "12:00-14:00,20:00-23:00";
        private String applyResults = "12:00-14:00,20:00-23:00";
        private String settleDraws = "12:00-15:00,20:00-23:30";
        private String closeDraws = "11:30-14:00,19:30-23:00";
        private String openDraws = "02:00-06:00";
    }


    @Getter
    @Setter
    public static class Scheduler {
        private boolean active = true;
        private Windows windows = new Windows();
        private Generate generate = new Generate();
        private OpenToday openToday = new OpenToday();
        private Processing processing = new Processing();
    }

    @Getter
    @Setter
    public static class Generate {
        private boolean active = true;
        private String cron = "0 0 5 * * *";
        private int daysAhead = 7;
        private int maxTenantsPerRun = 1000;
    }

    @Getter
    @Setter
    public static class OpenToday {
        private boolean active = true;
        private String cron = "0 */5 4-10 * * *";
        private LocalTime defaultSalesOpenTime = LocalTime.of(5, 30);
        private int maxItemsPerRun = 10000;
    }

    @Getter
    @Setter
    public static class Processing {
        private boolean active = true;
        private String cron = "0 */5 * * * *";
        private ZoneId timezone = ZoneId.of("America/New_York");
        private Close close = new Close();
        private Fetch fetch = new Fetch();
        private Apply apply = new Apply();
        private Settle settle = new Settle();
    }

    @Getter
    @Setter
    public static class Close {
        private boolean active = true;
        private int maxItemsPerTick = 500;
    }

    @Getter
    @Setter
    public static class DueAfterDraw {
        private boolean active = true;
        private int startMinutesAfterDraw;
        private int retryEveryMinutes;
        private int stopMinutesAfterDraw;
    }

    @Getter
    @Setter
    public static class Fetch extends DueAfterDraw {
        private int maxSlotsPerTick = 10;

        public Fetch() {
            setStartMinutesAfterDraw(5);
            setRetryEveryMinutes(10);
            setStopMinutesAfterDraw(240);
        }
    }

    @Getter
    @Setter
    public static class Apply extends DueAfterDraw {
        private int maxItemsPerTick = 500;

        public Apply() {
            setStartMinutesAfterDraw(10);
            setRetryEveryMinutes(30);
            setStopMinutesAfterDraw(720);
        }
    }

    @Getter
    @Setter
    public static class Settle extends DueAfterDraw {
        private int maxItemsPerTick = 1000;

        public Settle() {
            setStartMinutesAfterDraw(20);
            setRetryEveryMinutes(30);
            setStopMinutesAfterDraw(1440);
        }
    }


    @Getter
    @Setter
    public static class Lifecycle {
        private boolean active = true;
        private String generateCron = "0 0 5 * * *";
        private String openCloseCron = "0 */5 * * * *";
        private int generationDays = 7;
        private int batchSize = 500;
        private int lookaheadHours = 24;
        private int lagHours = 12;
    }

    @Getter
    @Setter
    public static class Watchdog {
        private boolean active = true;
        private int provisionalStuckMinutes = 30;
        private String provisionalCron = "0 */15 * * * *";
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
