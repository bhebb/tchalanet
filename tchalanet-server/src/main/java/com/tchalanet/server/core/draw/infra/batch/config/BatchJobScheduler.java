package com.tchalanet.server.core.draw.infra.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class BatchJobScheduler {

    private final DrawResultsJobStarter drawResultsJobStarter;
    private final DrawSettleJobStarter drawSettleJobStarter;
    private final Clock clock;

    // --------------------
    // FETCH (5 min after)
    // --------------------
    @Scheduled(cron = "0 35 14 * * *", zone = "America/New_York") // NY 14:30 -> 14:35
    public void fetchNyMidday() {
        startFetch("US_NY_NUM3_MID", 1, 300, false);
        startFetch("US_NY_NUM4_MID", 1, 300, false);
    }

    @Scheduled(cron = "0 35 22 * * *", zone = "America/New_York") // NY 22:30 -> 22:35
    public void fetchNyEvening() {
        startFetch("US_NY_NUM3_EVE", 1, 400, false);
        startFetch("US_NY_NUM4_EVE", 1, 400, false);
    }

    @Scheduled(cron = "0 35 13 * * *", zone = "America/New_York") // FL 13:30 -> 13:35
    public void fetchFloridaMidday() {
        startFetch("US_FL_NUM3_MID", 1, 400, false);
        startFetch("US_FL_NUM4_MID", 1, 400, false);
    }

    @Scheduled(cron = "0 50 22 * * *", zone = "America/New_York") // FL 22:45 -> 22:50
    public void fetchFloridaEvening() {
        startFetch("US_FL_NUM3_EVE", 1, 500, false);
        startFetch("US_FL_NUM4_EVE", 1, 500, false);
    }

    @Scheduled(cron = "0 20 23 * * *", zone = "America/New_York") // Florida Lotto 23:15 -> 23:20
    public void fetchFloridaLotto() {
        startFetch("US_FL_LOTTO_EVE", 1, 200, false);
    }

    // --------------------
    // SETTLE (10 min after)
    // --------------------
    @Scheduled(cron = "0 40 14 * * *", zone = "America/New_York")
    public void settleNyMidday() {
        startSettle("US_LOTTERY", "NY", null, 1, 500, false);
    }

    @Scheduled(cron = "0 40 22 * * *", zone = "America/New_York")
    public void settleNyEvening() {
        startSettle("US_LOTTERY", "NY", null, 1, 700, false);
    }

    @Scheduled(cron = "0 40 13 * * *", zone = "America/New_York")
    public void settleFloridaMidday() {
        startSettle("US_LOTTERY", "FLORIDA", null, 1, 700, false);
    }

    @Scheduled(cron = "0 55 22 * * *", zone = "America/New_York")
    public void settleFloridaEvening() {
        startSettle("US_LOTTERY", "FLORIDA", null, 1, 900, false);
    }

    // --------------------
    // helpers
    // --------------------
    private void startFetch(String channelCode,
                            int daysBack, int maxDraws, boolean dryRun) {
        var params = new java.util.HashMap<String, String>();
        params.put("ts", Long.toString(java.time.Instant.now(clock).toEpochMilli()));

        // MVP: tenant_id requis (demo)
        params.put("tenant_id", "00000000-0000-0000-0000-000000000002");

        params.put("channel_code", channelCode);
        params.put("days_back", Integer.toString(daysBack));
        params.put("max_draws", Integer.toString(maxDraws));
        params.put("dry_run", Boolean.toString(dryRun));
        params.put("force", "false");

        drawResultsJobStarter.startFetchDrawResultsJob(params);
    }

    private void startSettle(String source, String provider, String channelCode,
                             int daysBack, int maxDraws, boolean dryRun) {
        var params = buildParams(source, provider, channelCode, daysBack, maxDraws, dryRun);
        drawSettleJobStarter.startSettleDrawsJob(params);
    }

    private HashMap<String, String> buildParams(String source, String provider, String channelCode, int daysBack, int maxDraws, boolean dryRun) {
        var params = new HashMap<String, String>();
        params.put("ts", Long.toString(java.time.Instant.now(clock).toEpochMilli()));
        params.put("source", source);
        params.put("provider", provider);
        if (channelCode != null) params.put("channel_code", channelCode);
        params.put("days_back", Integer.toString(daysBack));
        params.put("max_draws", Integer.toString(maxDraws));
        params.put("dry_run", Boolean.toString(dryRun));
        // MVP: tenant_id requis (demo) pour settle, aligné sur fetch
        params.put("tenant_id", "00000000-0000-0000-0000-000000000002");
        // force optionnelle pour aligner avec DrawSettleJobStarter
        params.put("force", "false");
        return params;
    }
}
