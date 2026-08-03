package com.tchalanet.server.features.ops.analytics;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.http.TchHeaders;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.analytics.api.command.ReconcileAnalyticsCommand;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationMode;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationResult;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.idempotence.api.IdempotencyRequestHasher;
import com.tchalanet.server.platform.idempotence.api.IdempotencyStore;
import com.tchalanet.server.platform.idempotence.api.error.IdempotencyErrorCodes;
import com.tchalanet.server.platform.idempotence.api.model.IdempotencyScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Restricted operational repair surface. Tenant admins and cashiers never receive this endpoint. */
@RestController
@RequestMapping("/platform/ops/analytics/reconciliation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Ops • Analytics reconciliation")
public class AnalyticsReconciliationOpsController {

  private final CommandBus commandBus;
  private final IdempotencyStore idempotencyStore;
  private final JsonUtils jsonUtils;

  @Operation(summary = "Validate or explicitly rebuild tenant analytics projections")
  @PostMapping
  @AuditLog(
      entity = AuditEntityType.SYSTEM,
      action = AuditAction.OPS_FORCE_JOB,
      idExpression = "#request.tenantId().toString()",
      detailsExpression = "#request")
  public ApiResponse<AnalyticsReconciliationResult> reconcile(
      @Valid @RequestBody AnalyticsReconciliationRequest request,
      @RequestHeader(value = TchHeaders.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
      @CurrentContext TchRequestContext context) {
    return TchContextScope.runWithContextResult(
        context.withEffectiveTenantUuid(request.tenantId()),
        () -> reconcileForTenant(request, idempotencyKey));
  }

  private ApiResponse<AnalyticsReconciliationResult> reconcileForTenant(
      AnalyticsReconciliationRequest request, String idempotencyKey) {
    if (request.mode() == AnalyticsReconciliationMode.REBUILD_AND_VALIDATE
        && !StringUtils.hasText(idempotencyKey)) {
      throw ProblemRest.of(IdempotencyErrorCodes.MISSING);
    }

    if (request.mode() == AnalyticsReconciliationMode.VALIDATE) {
      return ApiResponse.success(execute(request));
    }

    String key = idempotencyKey.trim();
    String requestHash = IdempotencyRequestHasher.sha256Normalized(jsonUtils, request);
    var begin =
        idempotencyStore.begin(
            IdempotencyScope.ANALYTICS_RECONCILIATION, key, requestHash, 24 * 60 * 60L);

    return switch (begin.decision()) {
      case PAYLOAD_MISMATCH -> throw ProblemRest.of(IdempotencyErrorCodes.PAYLOAD_MISMATCH);
      case IN_PROGRESS -> throw ProblemRest.of(IdempotencyErrorCodes.IN_PROGRESS);
      case ALREADY_COMPLETED -> replayCompleted(begin);
      case STARTED -> executeAndRecord(request, key, requestHash);
    };
  }

  private ApiResponse<AnalyticsReconciliationResult> executeAndRecord(
      AnalyticsReconciliationRequest request, String key, String requestHash) {
    try {
      var result = execute(request);
      idempotencyStore.complete(
          IdempotencyScope.ANALYTICS_RECONCILIATION,
          key,
          requestHash,
          result.runId(),
          jsonUtils.toJson(result));
      return ApiResponse.success(result);
    } catch (RuntimeException exception) {
      idempotencyStore.fail(IdempotencyScope.ANALYTICS_RECONCILIATION, key, requestHash);
      throw exception;
    }
  }

  private ApiResponse<AnalyticsReconciliationResult> replayCompleted(
      IdempotencyStore.BeginResult begin) {
    var completed = begin.completed().orElseThrow();
    if (completed.responseJson() == null) {
      throw ProblemRest.of(IdempotencyErrorCodes.COMPLETED_NO_RESPONSE);
    }
    return ApiResponse.success(
        jsonUtils.readValue(completed.responseJson(), AnalyticsReconciliationResult.class));
  }

  private AnalyticsReconciliationResult execute(AnalyticsReconciliationRequest request) {
    return commandBus.execute(
        new ReconcileAnalyticsCommand(
            TenantId.of(request.tenantId()),
            request.from(),
            request.to(),
            request.mode(),
            request.repairReason()));
  }
}
