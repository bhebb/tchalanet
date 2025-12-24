package com.tchalanet.server.core.draw.infra.batch.results.fetch;

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
public class DrawResultsJobStarter {

    private final JobOperator jobOperator;

    private final Job fetchDrawResultsJob;

    @SneakyThrows
    public JobExecution startFetchDrawResultsJob(Map<String, String> params) {
        JobParameters jobParameters = buildJobParameters(params);
        return jobOperator.start(fetchDrawResultsJob, jobParameters);
    }

    private JobParameters buildJobParameters(Map<String, String> params) {
        // required
        require(params, "tenant_id");
        require(params, "channel_code");

        var builder = new JobParametersBuilder();
        builder.addLong("ts", parseLongOrNow(params.get("ts")), true);

        addString(builder, "tenant_id", params.get("tenant_id"));
        addString(builder, "channel_code", params.get("channel_code"));
        addLong(builder, "days_back", params.get("days_back"));
        addLong(builder, "max_draws", params.get("max_draws"));
        addString(builder, "force", params.getOrDefault("force", "false"));
        addString(builder, "dry_run", params.getOrDefault("dry_run", "false"));
        addString(builder, "draw_date", params.get("draw_date"));

        // ops metadata
        addString(builder, "ops_trigger", params.get("ops_trigger"));
        addString(builder, "triggered_by", params.get("triggered_by"));
        addString(builder, "request_id", params.get("request_id"));

        return builder.toJobParameters();
    }

    private static void require(Map<String, String> params, String key) {
        var v = params.get(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException(key + " is required");
    }

    private static long parseLongOrNow(String s) {
        try {
            return (s == null || s.isBlank()) ? System.currentTimeMillis() : Long.parseLong(s);
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private static void addString(JobParametersBuilder b, String key, String value) {
        if (value != null && !value.isBlank()) b.addString(key, value);
    }

    private static void addLong(JobParametersBuilder b, String key, String value) {
        if (value != null && !value.isBlank()) b.addLong(key, Long.parseLong(value), false);
    }
}
