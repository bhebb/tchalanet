package com.tchalanet.server.features.ops.web;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.ops.OpsBatchService;
import com.tchalanet.server.features.ops.model.GateUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Ops controller for batch gate management (enable/disable).
 *
 * NOTE: /api/v1 prefix is added automatically by platform config.
 *
 * Endpoints:
 * - GET  /platform/ops/batch/gates/{jobKey}?tenant_id=...
 * - GET  /platform/ops/batch/gates:effective?job_keys=...&tenant_id=...
 * - PUT  /platform/ops/batch/gates/{jobKey}
 */
@RestController
@RequestMapping("/platform/ops/batch/gates")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Ops • Batch Gate")
public class OpsBatchGateController {

    private final OpsBatchService batchService;

    // Optional default keys for convenience (UI can omit job_keys)
    private static final List<String> DEFAULT_JOB_KEYS = List.of(
        "draw:lifecycle:generate",
        "draw:lifecycle:open",
        "draw:lifecycle:close",
        "draw:lifecycle:settle",
        "results:external:refresh",
        "results:external:fetch",
        "results:external:apply",
        "results:external:manual",
        "results:external:override",
        "catalog:search:reindex"
    );

    @Operation(summary = "Get gate status for one job (effective + provenance)")
    @GetMapping("/{jobKey}")
    public ApiResponse<Map<String, Object>> getGate(
        @PathVariable String jobKey,
        @RequestParam(required = false, name = "tenant_id") String tenantId
    ) {
        return ApiResponse.success(batchService.getGateStatus(jobKey, tenantId));
    }

    @Operation(summary = "Get effective gate status for multiple jobs (bulk)")
    @GetMapping(":effective")
    public ApiResponse<Map<String, Boolean>> getEffectiveGates(
        @RequestParam(required = false, name = "job_keys") List<String> jobKeys,
        @RequestParam(required = false, name = "tenant_id") String tenantId
    ) {
        List<String> keys = (jobKeys != null && !jobKeys.isEmpty()) ? jobKeys : DEFAULT_JOB_KEYS;
        return ApiResponse.success(batchService.getGateStatusBulk(keys, tenantId));
    }

    @Operation(summary = "Update gate flag (enable/disable)")
    @PutMapping("/{jobKey}")
    public ApiResponse<Void> updateGate(
        @PathVariable String jobKey,
        @Valid @RequestBody GateUpdateRequest request
    ) {
        batchService.updateGate(jobKey, request);
        return ApiResponse.success(null);
    }
}
