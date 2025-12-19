package com.tchalanet.server.core.draw.infra.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawSettleJobStarter {

    private final JobOperator jobOperator;
    private final Job settleDrawsJob;

    @SneakyThrows
    public JobExecution startSettleDrawsJob(Map<String, String> params) {
        var jobParameters = buildJobParameters(params);
        return jobOperator.start(settleDrawsJob, jobParameters);
    }

    private JobParameters buildJobParameters(Map<String, String> params) {
        JobParametersBuilder builder = new JobParametersBuilder();

        // ts : identifiant principal
        if (params.containsKey("ts")) {
            builder.addLong("ts", Long.parseLong(params.get("ts")), true);
        } else {
            builder.addLong("ts", System.currentTimeMillis(), true);
        }

        // Flags / contexte
        addString(builder, "ops_trigger", params.get("ops_trigger"));
        addString(builder, "tenant_id", params.get("tenant_id"));
        addString(builder, "source", params.get("source"));
        addString(builder, "provider", params.get("provider"));
        addString(builder, "channel_code", params.get("channel_code"));
        addString(builder, "triggered_by", params.get("triggered_by"));
        addString(builder, "request_id", params.get("request_id"));

        // Limites / fenêtrage
        if (params.containsKey("days_back")) {
            builder.addLong("days_back", Long.parseLong(params.get("days_back")), false);
        }
        if (params.containsKey("max_draws")) {
            builder.addLong("max_draws", Long.parseLong(params.get("max_draws")), false);
        }

        // dry_run
        if (params.containsKey("dry_run")) {
            builder.addString("dry_run", params.get("dry_run"));
        }

        // force (si tu veux un comportement identique au fetch)
        if (params.containsKey("force")) {
            builder.addString("force", params.get("force"));
        }

        return builder.toJobParameters();
    }

    private void addString(JobParametersBuilder builder, String key, String value) {
        if (value != null && !value.isBlank()) {
            builder.addString(key, value);
        }
    }
}

