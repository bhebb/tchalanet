package com.tchalanet.server.features.ops.batch;

import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingEntity;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.history.BatchJobHistoryPurgeResult;
import com.tchalanet.server.common.job.history.BatchJobHistoryService;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.launch.BatchJobExecutionOperator;
import com.tchalanet.server.common.job.launch.BatchJobStarter;
import com.tchalanet.server.common.job.registry.RegisteredJob;
import com.tchalanet.server.common.job.registry.TchJobRegistry;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.features.ops.batch.model.ExecutionResponse;
import com.tchalanet.server.features.ops.batch.model.GateStatusResponse;
import com.tchalanet.server.features.ops.batch.model.GateUpdateRequest;
import com.tchalanet.server.features.ops.batch.model.JobInfoResponse;
import com.tchalanet.server.features.ops.batch.model.PurgeExecutionsRequest;
import com.tchalanet.server.features.ops.batch.model.PurgeExecutionsResponse;
import com.tchalanet.server.features.ops.batch.model.StartJobRequest;
import com.tchalanet.server.features.ops.batch.model.StartJobResponse;
import com.tchalanet.server.features.ops.error.OpsErrorCodes;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class OpsBatchService {

  private static final String BATCH_NAMESPACE = "batch";

  private final TchJobRegistry tchBatchJobRegistry;
  private final BatchGate gate;
  private final BatchJobStarter jobStarter;
  private final BatchJobExecutionOperator jobExecutionOperator;
  private final BatchJobHistoryService batchJobHistoryService;
  private final SettingRepository appSettingRepo;

  public OpsBatchService(
      TchJobRegistry tchBatchJobRegistry,
      BatchGate gate,
      BatchJobStarter jobStarter,
      BatchJobExecutionOperator jobExecutionOperator,
      BatchJobHistoryService batchJobHistoryService,
      SettingRepository appSettingRepo) {
    this.tchBatchJobRegistry = tchBatchJobRegistry;
    this.gate = gate;
    this.jobStarter = jobStarter;
    this.jobExecutionOperator = jobExecutionOperator;
    this.batchJobHistoryService = batchJobHistoryService;
    this.appSettingRepo = appSettingRepo;
  }

  public List<JobInfoResponse> listJobs() {
    return tchBatchJobRegistry.list().stream().map(this::toJobInfoResponse).toList();
  }

  public JobInfoResponse getJob(String jobKeyStr) {
    JobKey jobKey = parseJobKey(jobKeyStr);
    RegisteredJob registered =
        tchBatchJobRegistry
            .find(jobKey)
            .orElseThrow(() -> ProblemRest.of(OpsErrorCodes.BATCH_JOB_NOT_FOUND));
    return toJobInfoResponse(registered);
  }

  public StartJobResponse startJob(String jobKeyStr, StartJobRequest request) {
    JobKey jobKey = parseJobKey(jobKeyStr);

    log.info("ops.batch.start.request jobKey={} paramsCount={}", jobKey, request.params().size());

    var execution = jobStarter.start(jobKey, request.params());

    return new StartJobResponse(
        jobKey.value(),
        parseExecutionId(execution.jobExecutionId()),
        execution.status(),
        Instant.now());
  }

  public GateStatusResponse getGateStatus(String jobKeyStr, String tenantIdStr) {
    JobKey jobKey = parseJobKey(jobKeyStr);

    TenantId tenantId = null;
    if (tenantIdStr != null && !tenantIdStr.isBlank()) {
      tenantId = parseTenantId(tenantIdStr);
    }

    var enabled = gate.enabled(jobKey, tenantId);

    return new GateStatusResponse(
        jobKey.value(),
        enabled,
        tenantId == null ? "GLOBAL_OR_DEFAULT" : "TENANT_OR_DEFAULT",
        tenantId != null ? tenantId.toString() : null);
  }

  public Map<String, Boolean> getGateStatusBulk(List<String> jobKeys, String tenantIdStr) {
    TenantId tenantId = null;
    if (tenantIdStr != null && !tenantIdStr.isBlank()) {
      tenantId = parseTenantId(tenantIdStr);
    }

    Map<String, Boolean> result = new LinkedHashMap<>();
    for (String raw : jobKeys) {
      try {
        JobKey jk = parseJobKey(raw);
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
    JobKey jobKey = parseJobKey(jobKeyStr);

    tchBatchJobRegistry
        .find(jobKey)
        .orElseThrow(() -> ProblemRest.of(OpsErrorCodes.BATCH_JOB_NOT_FOUND));

    log.info(
        "ops.batch.gate.update jobKey={} scope={} enabled={} reason={}",
        jobKey,
        request.scope(),
        request.enabled(),
        request.reason());

    SettingLevel level;
    TenantId tenantId = null;

    if ("TENANT".equalsIgnoreCase(request.scope())) {
      level = SettingLevel.TENANT;
      if (request.tenant_id() == null || request.tenant_id().isBlank()) {
        throw ProblemRest.of(OpsErrorCodes.BATCH_TENANT_REQUIRED);
      }
      tenantId = parseTenantId(request.tenant_id());
    } else if ("GLOBAL".equalsIgnoreCase(request.scope())) {
      level = SettingLevel.GLOBAL;
      if (request.tenant_id() != null && !request.tenant_id().isBlank()) {
        throw ProblemRest.of(OpsErrorCodes.BATCH_TENANT_NOT_ALLOWED);
      }
    } else {
      throw ProblemRest.of(OpsErrorCodes.BATCH_SCOPE_INVALID);
    }

    String settingKey = "jobs." + jobKey.value() + ".enabled";
    UUID tenantUuid = tenantId != null ? tenantId.value() : null;

    Optional<SettingEntity> existing =
        appSettingRepo
            .findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndNamespaceAndSettingKey(
                level, tenantUuid, BATCH_NAMESPACE, settingKey);

    SettingEntity entity = existing.orElseGet(SettingEntity::new);
    entity.setLevel(level);
    entity.setTenantId(tenantUuid);
    entity.setNamespace(BATCH_NAMESPACE);
    entity.setSettingKey(settingKey);
    entity.setValueType(SettingValueType.BOOLEAN);
    entity.setSettingValue(Boolean.toString(request.enabled()));
    entity.setActive(true);

    appSettingRepo.save(entity);

    log.info(
        "ops.batch.gate.updated jobKey={} scope={} enabled={}",
        jobKey,
        request.scope(),
        request.enabled());
  }

  public ExecutionResponse getExecution(long executionId) {
    return batchJobHistoryService
        .getExecution(executionId)
        .map(
            view ->
                new ExecutionResponse(
                    view.executionId(),
                    view.jobKey(),
                    view.status(),
                    view.startedAt(),
                    view.endedAt(),
                    view.context(),
                    view.exitCode(),
                    view.exitMessage()))
        .orElseThrow(() -> ProblemRest.of(OpsErrorCodes.BATCH_EXECUTION_NOT_FOUND));
  }

  public StartJobResponse restartExecution(long executionId) {
    batchJobHistoryService
        .getExecution(executionId)
        .orElseThrow(() -> ProblemRest.of(OpsErrorCodes.BATCH_EXECUTION_NOT_FOUND));

    var execution = jobExecutionOperator.restart(executionId);

    return new StartJobResponse(
        null, parseExecutionId(execution.jobExecutionId()), execution.status(), Instant.now());
  }

  public List<ExecutionResponse> listExecutions(String jobKeyStr, int limit) {
    if (jobKeyStr == null || jobKeyStr.isBlank()) {
      throw ProblemRest.of(OpsErrorCodes.BATCH_JOB_KEY_REQUIRED);
    }
    if (limit < 1 || limit > 200) {
      throw ProblemRest.of(OpsErrorCodes.BATCH_LIMIT_INVALID);
    }

    JobKey jobKey = parseJobKey(jobKeyStr);
    tchBatchJobRegistry
        .find(jobKey)
        .orElseThrow(() -> ProblemRest.of(OpsErrorCodes.BATCH_JOB_NOT_FOUND));

    return batchJobHistoryService.listExecutions(jobKey, limit).stream()
        .map(
            view ->
                new ExecutionResponse(
                    view.executionId(),
                    view.jobKey(),
                    view.status(),
                    view.startedAt(),
                    view.endedAt(),
                    view.context(),
                    view.exitCode(),
                    view.exitMessage()))
        .toList();
  }

  public PurgeExecutionsResponse purgeExecutions(PurgeExecutionsRequest request) {
    int retentionDays;
    try {
      retentionDays = request != null ? request.resolvedRetentionDays() : 7;
    } catch (IllegalArgumentException exception) {
      throw ProblemRest.of(OpsErrorCodes.BATCH_RETENTION_INVALID, Map.of(), exception);
    }
    Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
    BatchJobHistoryPurgeResult result = batchJobHistoryService.purgeBefore(cutoff);
    return new PurgeExecutionsResponse(
        result.cutoff(),
        result.jobExecutionContextRows(),
        result.stepExecutionContextRows(),
        result.stepExecutionRows(),
        result.jobExecutionParamRows(),
        result.jobExecutionRows(),
        result.jobInstanceRows());
  }

  private JobInfoResponse toJobInfoResponse(RegisteredJob registered) {
    return new JobInfoResponse(
        registered.jobKey().value(),
        registered.displayName(),
        registered.scope().name(),
        registered.requiredParams(),
        registered.optionalParams());
  }

  private static long parseExecutionId(String executionId) {
    if (executionId == null || executionId.isBlank()) {
      return 0L;
    }
    return Long.parseLong(executionId);
  }

  private static JobKey parseJobKey(String raw) {
    try {
      return JobKey.of(raw);
    } catch (IllegalArgumentException exception) {
      throw ProblemRest.of(OpsErrorCodes.BATCH_JOB_KEY_INVALID, Map.of(), exception);
    }
  }

  private static TenantId parseTenantId(String raw) {
    try {
      return TenantId.parse(raw);
    } catch (IllegalArgumentException exception) {
      throw ProblemRest.of(OpsErrorCodes.BATCH_TENANT_ID_INVALID, Map.of(), exception);
    }
  }
}
