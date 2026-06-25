package com.tchalanet.server.features.ops.batch;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.ops.batch.model.ExecutionResponse;
import com.tchalanet.server.features.ops.batch.model.PurgeExecutionsRequest;
import com.tchalanet.server.features.ops.batch.model.PurgeExecutionsResponse;
import com.tchalanet.server.features.ops.batch.model.StartJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ops controller for batch execution queries.
 * NOTE: /api/v1 prefix is added automatically by platform config.
 * Endpoints:
 * - GET /platform/ops/batch/executions/{executionId}
 * - GET /platform/ops/batch/executions?job_key=...&limit=20
 */
@RestController
@RequestMapping("/platform/ops/batch/executions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Ops • Batch Executions")
public class OpsBatchExecutionController {

    private final OpsBatchService batchService;

    @Operation(summary = "Get execution details")
    @GetMapping("/{executionId}")
    public ApiResponse<ExecutionResponse> getExecution(@PathVariable long executionId) {
        return ApiResponse.success(batchService.getExecution(executionId));
    }

    @Operation(summary = "Restart a failed/stopped batch execution")
    @PostMapping("/{executionId}:restart")
    public ApiResponse<StartJobResponse> restartExecution(@PathVariable long executionId) {
        return ApiResponse.success(batchService.restartExecution(executionId));
    }

    @Operation(summary = "List recent executions for a given job_key")
    @GetMapping
    public ApiResponse<List<ExecutionResponse>> listExecutions(
        @RequestParam(name = "job_key") String jobKey,
        @RequestParam(required = false, defaultValue = "20") int limit
    ) {
        return ApiResponse.success(batchService.listExecutions(jobKey, limit));
    }

    @Operation(summary = "Purge old batch execution history")
    @PostMapping(":purge")
    public ApiResponse<PurgeExecutionsResponse> purgeExecutions(
        @Valid @RequestBody(required = false) PurgeExecutionsRequest request
    ) {
        return ApiResponse.success(batchService.purgeExecutions(request));
    }
}
