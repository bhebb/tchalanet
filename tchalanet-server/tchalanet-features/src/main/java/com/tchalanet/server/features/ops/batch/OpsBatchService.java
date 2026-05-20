package com.tchalanet.server.features.ops.batch;

import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingEntity;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.launch.BatchJobStarter;
import com.tchalanet.server.common.job.registry.RegisteredJob;
import com.tchalanet.server.common.job.registry.TchJobRegistry;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.features.ops.batch.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class OpsBatchService {

    private static final String BATCH_NAMESPACE = "batch";

    private final TchJobRegistry tchBatchJobRegistry;
    private final BatchGate gate;
    private final BatchJobStarter jobStarter;
    private final SettingRepository appSettingRepo;

    public OpsBatchService(
        TchJobRegistry tchBatchJobRegistry,
        BatchGate gate,
        BatchJobStarter jobStarter,
        SettingRepository appSettingRepo
    ) {
        this.tchBatchJobRegistry = tchBatchJobRegistry;
        this.gate = gate;
        this.jobStarter = jobStarter;
        this.appSettingRepo = appSettingRepo;
    }

    public List<JobInfoResponse> listJobs() {
        return tchBatchJobRegistry.list().stream()
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

        return new StartJobResponse(
            jobKey.value(),
            parseExecutionId(execution.jobExecutionId()),
            execution.status(),
            Instant.now()
        );
    }

    public Map<String, Object> getGateStatus(String jobKeyStr, String tenantIdStr) {
        JobKey jobKey = JobKey.of(jobKeyStr);

        TenantId tenantId = null;
        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
            tenantId = TenantId.parse(tenantIdStr);
        }

        var enabled = gate.enabled(jobKey, tenantId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("job_key", jobKey.value());
        out.put("enabled", enabled);
        out.put("scope", tenantId == null ? "GLOBAL_OR_DEFAULT" : "TENANT_OR_DEFAULT");
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
                result.put(raw, gate.enabled(jk, tenantId));
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

        log.info("ops.batch.gate.updated jobKey={} scope={} enabled={}",
            jobKey, request.scope(), request.enabled());
    }

    public ExecutionResponse getExecution(long executionId) {
        throw batchHistoryNotExposed();
    }

    public List<ExecutionResponse> listExecutions(String jobKeyStr, int limit) {
        if (jobKeyStr == null || jobKeyStr.isBlank()) {
            throw new IllegalArgumentException("job_key is required");
        }
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("limit must be between 1 and 200");
        }

        JobKey jobKey = JobKey.of(jobKeyStr);
        tchBatchJobRegistry.find(jobKey)
            .orElseThrow(() -> new IllegalArgumentException("Job not in allowlist: " + jobKey));

        throw batchHistoryNotExposed();
    }

    private static UnsupportedOperationException batchHistoryNotExposed() {
        return new UnsupportedOperationException(
            "Batch execution history is owned by app.batch runtime and is not exposed through common.job contracts yet"
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

    private static long parseExecutionId(String executionId) {
        if (executionId == null || executionId.isBlank()) {
            return 0L;
        }
        return Long.parseLong(executionId);
    }
}
