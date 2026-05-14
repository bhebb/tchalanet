package com.tchalanet.server.features.ops.batch;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingEntity;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import com.tchalanet.server.common.batch.gate.BatchGateCache;
import com.tchalanet.server.common.batch.gate.BatchGateResolver;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.batch.launch.BatchJobStarter;
import com.tchalanet.server.common.batch.registry.TchBatchJobRegistry;
import com.tchalanet.server.common.batch.registry.RegisteredJob;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.features.ops.batch.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
public class OpsBatchService {

    private static final String BATCH_NAMESPACE = "batch";

    private final TchBatchJobRegistry tchBatchJobRegistry;
    private final BatchGateResolver gateResolver;
    private final BatchGateCache gateCache;
    private final BatchJobStarter jobStarter;
    private final SettingRepository appSettingRepo;
    private final JobRepository jobRepository;

    public OpsBatchService(
        TchBatchJobRegistry tchBatchJobRegistry,
        BatchGateResolver gateResolver,
        BatchGateCache gateCache,
        BatchJobStarter jobStarter,
        SettingRepository appSettingRepo,
        JobRepository jobRepository
    ) {
        this.tchBatchJobRegistry = tchBatchJobRegistry;
        this.gateResolver = gateResolver;
        this.gateCache = gateCache;
        this.jobStarter = jobStarter;
        this.appSettingRepo = appSettingRepo;
        this.jobRepository = jobRepository;
    }

    public List<JobInfoResponse> listJobs() {
        return tchBatchJobRegistry.list().values().stream()
            .map(this::toJobInfoResponse)
            .toList();
    }

    public JobInfoResponse getJob(String jobKeyStr) {
        JobKey jobKey = JobKey.of(jobKeyStr);
        RegisteredJob registered = tchBatchJobRegistry.find(jobKey)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobKey));
        return toJobInfoResponse(registered);
    }

    public StartJobResponse startJob(String jobKeyStr, StartJobRequest request) {
        JobKey jobKey = JobKey.of(jobKeyStr);

        log.info("ops.batch.start.request jobKey={} paramsCount={}", jobKey, request.params().size());

        var execution = jobStarter.start(jobKey, request.params());

        var startLdt = execution.getStartTime();
        TchRequestContext ctxStart = TchContext.currentOrNull();
        ZoneId effectiveZoneStart = (ctxStart != null && ctxStart.tenantZoneId() != null) ? ctxStart.tenantZoneId() : ZoneId.of("UTC");
        Instant startedAt = startLdt != null ? ZonedDateTime.of(startLdt, effectiveZoneStart).toInstant() : Instant.now();

        return new StartJobResponse(
            jobKey.value(),
            execution.getId(),
            execution.getStatus().name(),
            startedAt
        );
    }

    public Map<String, Object> getGateStatus(String jobKeyStr, String tenantIdStr) {
        JobKey jobKey = JobKey.of(jobKeyStr);

        TenantId tenantId = null;
        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
            tenantId = TenantId.parse(tenantIdStr);
        }

        var res = gateResolver.resolveWithScope(jobKey, tenantId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("job_key", jobKey.value());
        out.put("enabled", res.enabled());
        out.put("scope", res.scope());
        out.put("tenant_id", tenantId != null ? tenantId.toString() : null);
        return out;
    }

    public Map<String, Boolean> getGateStatusBulk(List<String> jobKeys, String tenantIdStr) {
        TenantId tenantId = null;
        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
            tenantId = TenantId.parse(tenantIdStr);
        }

        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String raw : jobKeys) {
            try {
                JobKey jk = JobKey.of(raw);
                if (tchBatchJobRegistry.find(jk).isEmpty()) {
                    result.put(raw, false);
                    continue;
                }
                result.put(raw, gateResolver.resolve(jk, tenantId));
            } catch (Exception e) {
                log.warn("ops.batch.gate.bulk.error jobKey={}", raw, e);
                result.put(raw, false);
            }
        }
        return result;
    }

    @Transactional
    public void updateGate(String jobKeyStr, GateUpdateRequest request) {
        JobKey jobKey = JobKey.of(jobKeyStr);

        tchBatchJobRegistry.find(jobKey)
            .orElseThrow(() -> new IllegalArgumentException("Job not in allowlist: " + jobKey));

        log.info("ops.batch.gate.update jobKey={} scope={} enabled={} reason={}",
            jobKey, request.scope(), request.enabled(), request.reason());

        SettingLevel level;
        TenantId tenantId = null;

        if ("TENANT".equalsIgnoreCase(request.scope())) {
            level = SettingLevel.TENANT;
            if (request.tenant_id() == null || request.tenant_id().isBlank()) {
                throw new IllegalArgumentException("tenant_id required for TENANT scope");
            }
            tenantId = TenantId.parse(request.tenant_id());
        } else if ("GLOBAL".equalsIgnoreCase(request.scope())) {
            level = SettingLevel.GLOBAL;
            if (request.tenant_id() != null && !request.tenant_id().isBlank()) {
                throw new IllegalArgumentException("tenant_id not allowed for GLOBAL scope");
            }
        } else {
            throw new IllegalArgumentException("Invalid scope: " + request.scope() + " (TENANT|GLOBAL)");
        }

        String settingKey = "jobs." + jobKey.value() + ".enabled";
        UUID tenantUuid = tenantId != null ? tenantId.value() : null;

        Optional<SettingEntity> existing = appSettingRepo
            .findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndTerminalIdAndNamespaceAndSettingKey(
                level, tenantUuid, null, null, BATCH_NAMESPACE, settingKey);

        SettingEntity entity = existing.orElseGet(SettingEntity::new);
        entity.setLevel(level);
        entity.setTenantId(tenantUuid);
        entity.setOutletId(null);
        entity.setTerminalId(null);
        entity.setNamespace(BATCH_NAMESPACE);
        entity.setSettingKey(settingKey);
        entity.setValueType(SettingValueType.BOOLEAN);
        entity.setSettingValue(Boolean.toString(request.enabled()));
        entity.setActive(true);

        appSettingRepo.save(entity);

        if (tenantId != null) {
            gateCache.cacheTenant(jobKey, tenantId, request.enabled());
        } else {
            gateCache.cacheGlobal(jobKey, request.enabled());
        }

        log.info("ops.batch.gate.updated jobKey={} scope={} enabled={}",
            jobKey, request.scope(), request.enabled());
    }

    public ExecutionResponse getExecution(long executionId) {
        JobExecution exec = jobRepository.getJobExecution(executionId);
        if (exec == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }
        return toExecutionResponse(exec);
    }

    public List<ExecutionResponse> listExecutions(String jobKeyStr, int limit) {
        if (jobKeyStr == null || jobKeyStr.isBlank()) {
            throw new IllegalArgumentException("job_key is required");
        }
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("limit must be between 1 and 200");
        }

        JobKey jobKey = JobKey.of(jobKeyStr);
        RegisteredJob reg = tchBatchJobRegistry.find(jobKey)
            .orElseThrow(() -> new IllegalArgumentException("Job not in allowlist: " + jobKey));

        String jobName = reg.springJobBeanName();

        var instances = jobRepository.getJobInstances(jobName, 0, Math.min(50, limit));
        List<ExecutionResponse> out = new ArrayList<>(limit);

        for (var instance : instances) {
            var execs = jobRepository.getJobExecutions(instance);
            for (var exec : execs) {
                out.add(toExecutionResponse(exec));
                if (out.size() >= limit) {
                    return out;
                }
            }
        }
        return out;
    }

    private ExecutionResponse toExecutionResponse(JobExecution execution) {
        LocalDateTime startLdt = execution.getStartTime();
        LocalDateTime endLdt = execution.getEndTime();

        TchRequestContext ctx = TchContext.currentOrNull();
        ZoneId effectiveZone = (ctx != null && ctx.tenantZoneId() != null) ? ctx.tenantZoneId() : ZoneId.of("UTC");

        Instant startedAt = startLdt != null ? ZonedDateTime.of(startLdt, effectiveZone).toInstant() : Instant.EPOCH;
        Instant endedAt = endLdt != null ? ZonedDateTime.of(endLdt, effectiveZone).toInstant() : null;

        return new ExecutionResponse(
            execution.getId(),
            execution.getJobInstance().getJobName(),
            execution.getStatus().name(),
            startedAt,
            endedAt
        );
    }

    private JobInfoResponse toJobInfoResponse(RegisteredJob registered) {
        return new JobInfoResponse(
            registered.jobKey().value(),
            registered.displayName(),
            registered.scope().name(),
            registered.requiredParams(),
            registered.optionalParams()
        );
    }
}
